import enums.UserTransactionsReasons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClanGoldManagement {
    private static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USERNAME = "skytecgames";
    private static final String DB_PASSWORD = "skytecgames";
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS clans (id INT PRIMARY KEY, name VARCHAR(255), gold INT)";
    private static final String CREATE_USERS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(255), gold INT)";
    private static final String CREATE_TASKS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS tasks (id INT PRIMARY KEY, name VARCHAR(255))";
    private static final String SELECT_CLAN_QUERY = "SELECT * FROM clans WHERE id = ?";
    private static final String SELECT_USER_GOLD_QUERY = "SELECT gold FROM users WHERE id = ?";
    private static final String UPDATE_USER_GOLD_QUERY = "UPDATE users SET gold = ? WHERE id = ?";
    private static final String UPDATE_CLAN_GOLD_QUERY = "UPDATE clans SET gold = ? WHERE id = ?";
    private static final String SELECT_TASK_QUERY = "SELECT * FROM tasks WHERE id = ?";
    private static final Logger logger = LoggerFactory.getLogger(ClanGoldManagement.class);

    private final ExecutorService executorService;
    private Connection connection;

    public ClanGoldManagement() {
        executorService = Executors.newFixedThreadPool(100);
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            connection.createStatement().execute(CREATE_TABLE_QUERY);
            connection.createStatement().execute(CREATE_USERS_TABLE_QUERY);
            connection.createStatement().execute(CREATE_TASKS_TABLE_QUERY);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void addGoldToClan(long userId, long clanId, int gold, UserTransactionsReasons reasons) {
        executorService.execute(() -> {
            Connection connection = null;
            PreparedStatement userGoldStatement = null;
            PreparedStatement clanGoldStatement = null;

            try {
                connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                connection.setAutoCommit(false);

                // Проверка, что у пользователя есть достаточно золота в случае если он просто переведёт свои средства клану
                int userGold = getUserGold(connection, userId);
                if (userGold < gold) {
                    String message = "У пользователя недостаточно золота.";
                    System.out.println(message);
                    String logMessage = String.format("%s ID '%d'", message, userId);
                    logger.debug(logMessage);
                    connection.rollback(); // Откатываем транзакцию
                    return;
                }
                int newUserGold = userGold - gold;
                updateUserGold(connection, userId, newUserGold);


                Clan clan = getClan(connection, clanId);
                if (clan != null) {
                    int oldGold = clan.getGold();
                    int newGold = oldGold + gold;
                    updateClanGold(connection, clanId, newGold);

                    String message = String.format("Золото успешно добавлено в казну клана. Клан: '%s', Пользователь:" +
                                    " '%d', Добавленное золото: '%d', Текущее количество золота в казне: '%d'",
                            clan.getName(), userId, gold, newGold);
                    System.out.println(message);

                    String logMessage = String.format("%s, Причина: '%s', Предыдущее количество золота в казне: '%d'",
                            message, reasons.name(), oldGold);
                    logger.debug(logMessage);
                }

                connection.commit(); // Подтверждаем транзакцию
            } catch (SQLException e) {
                try {
                    if (connection != null) {
                        connection.rollback();
                    }
                } catch (SQLException ex) {
                    logger.error(e.getMessage());
                    ex.printStackTrace();
                }
                logger.error(e.getMessage());
                e.printStackTrace();
            } finally {
                closeResources(connection, userGoldStatement, clanGoldStatement);
            }
        });
    }

    public void completeTask(long userId, long clanId, long taskId) {
        executorService.execute(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
                connection.setAutoCommit(false);

                Clan clan = getClan(connection, clanId);
                if (clan == null) {
                    String message = String.format("Клан с идентификатором '%d' не найден.", clanId);
                    System.out.println(message);
                    String logMessage = String.format("%s Идентификатор пользователя: '%d'", message, userId);
                    logger.debug(logMessage);
                    return;
                }

                Task task = getTask(connection, taskId);
                if (task == null) {
                    String message = String.format("Задача с идентификатором '%d' не найдена.", taskId);
                    System.out.println(message);
                    String logMessage = String.format("%s Идентификатор пользователя: '%d'", message, userId);
                    logger.debug(logMessage);
                    return;
                }
                int reward = task.getReward();
                int oldGold = clan.getGold();
                int newGold = oldGold + reward;
                // Добавление золота казне клана
                updateClanGold(connection, clanId, newGold);
                String message = String.format("Задание успешно выполнено пользователем. Пользователь: '%d'," +
                                " Задание: '%s', Награда: '%d', Текущее количество золота в казне: '%d'",
                        userId, task.getName(), reward, newGold);
                System.out.println(message);
                String logMessage = String.format("%s, Предыдущее количество золота в казне: '%d'", message, oldGold);
                logger.debug(logMessage);
            } catch (SQLException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        });
    }


    private Task getTask(Connection connection, long taskId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_TASK_QUERY)) {
            statement.setLong(1, taskId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                long id = resultSet.getLong("id");
                String name = resultSet.getString("name");
                int reward = resultSet.getInt("reward");
                return new Task(id, name, reward);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private int getUserGold(Connection connection, long userId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_USER_GOLD_QUERY);
        preparedStatement.setLong(1, userId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt("gold");
        }
        return 0;
    }

    private void updateUserGold(Connection connection, long userId, int newGold) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_GOLD_QUERY);
        preparedStatement.setInt(1, newGold);
        preparedStatement.setLong(2, userId);
        preparedStatement.executeUpdate();
    }

    private Clan getClan(Connection connection, long clanId) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CLAN_QUERY);
        preparedStatement.setLong(1, clanId);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            Clan clan = new Clan();
            clan.setId(resultSet.getLong("id"));
            clan.setName(resultSet.getString("name"));
            clan.setGold(resultSet.getInt("gold"));
            return clan;
        }
        return null;
    }

    private void updateClanGold(Connection connection, long clanId, int newGold) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_CLAN_GOLD_QUERY);
        preparedStatement.setInt(1, newGold);
        preparedStatement.setLong(2, clanId);
        preparedStatement.executeUpdate();
    }

    private void closeResources(Connection connection, PreparedStatement preparedStatement1, PreparedStatement preparedStatement2) {
        try {
            if (preparedStatement1 != null) {
                preparedStatement1.close();
            }

            if (preparedStatement2 != null) {
                preparedStatement2.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
