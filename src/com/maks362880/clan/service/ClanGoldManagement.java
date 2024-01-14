import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
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
    private static final String SELECT_USER_QUERY = "SELECT * FROM users WHERE id = ?";
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

            //connection.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void addGoldToClan(long userId, long clanId, int gold) {
        //Формируем точу отката, она в будущем поможет уменьшить количество фантомных ошибок
        // т.к. б.д. h2 у нас на уровне изоляции read commit
        Savepoint rollbackPoint = null;
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            connection.setAutoCommit(false);
            rollbackPoint = connection.setSavepoint();

            // Проверяем, достаточно ли золота у пользователя
            int userGoldAmount = getUserGoldAmount(connection, userId);
            if (userGoldAmount < gold) {
                String message = "У пользователя недостаточно золота.";
                logger.debug("{} ID '{}'", message, userId);
                return;
            }

            // Проверяем существование пользователя и клана
            if (!userExists(connection, userId) || !clanExists(connection, clanId)) {
                String message = "Пользователь или клан не существуют.";
                logger.debug(message);
                return;
            }

            // Обновляем золото пользователя и золото клана
            int newUserGoldAmount = userGoldAmount - gold;
            updateUserGoldAmount(connection, userId, newUserGoldAmount);
            updateClanGold(connection, clanId, gold);

            String message = String.format("Золото успешно добавлено в казну клана. Клан: '%s'," +
                            " Пользователь: '%d', Добавленное золото: '%d', Текущее количество золота в казне: '%d'",
                    getClanName(connection, clanId), userId, gold, getClanGold(connection, clanId));
            logger.debug(message);

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback(rollbackPoint);
            } catch (SQLException ex) {
                logger.error("Ошибка при откате транзакции: {}", ex.getMessage());
            }
            logger.error("Ошибка при добавлении золота в казну клана: {}", e.getMessage());
        }
    }

    private int getUserGoldAmount(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_USER_GOLD_QUERY)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("gold");
                }
            }
        }
        return 0;
    }

    private void updateUserGoldAmount(Connection connection, long userId, int newGoldAmount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_USER_GOLD_QUERY)) {
            statement.setInt(1, newGoldAmount);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    private boolean userExists(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_USER_QUERY)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean clanExists(Connection connection, long clanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private String getClanName(Connection connection, long clanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getString("name");
                }
            }
        }
        return null;
    }

    private int getClanGold(Connection connection, long clanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("gold");
                }
            }
        }
        return 0;
    }

    private void updateClanGold(Connection connection, long clanId, int goldAdded) throws SQLException {
        int newGoldAmount = getClanGold(connection, clanId) + goldAdded;
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_CLAN_GOLD_QUERY)) {
            statement.setInt(1, newGoldAmount);
            statement.setLong(2, clanId);
            statement.executeUpdate();
        }
    }




    public void completeTask(long userId, long clanId, long taskId) {
        executorService.execute(() -> {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
                connection.setAutoCommit(false);
                // Создаем savepoint для возможной отмены изменений
                Savepoint rollbackPoint = connection.setSavepoint();

                Clan clan = getClan(connection, clanId);
                if (clan == null) {
                    String message = String.format("Клан с идентификатором '%d' не найден.", clanId);
                    System.out.println(message);
                    String logMessage = String.format("%s Идентификатор пользователя: '%d'", message, userId);
                    logger.debug(logMessage);
                    // Откатываем изменения до savepoint, чтобы отменить частично выполненные изменения
                    connection.rollback(rollbackPoint);
                    return;
                }

                Task task = getTask(connection, taskId);
                if (task == null) {
                    String message = String.format("Задача с идентификатором '%d' не найдена.", taskId);
                    System.out.println(message);
                    String logMessage = String.format("%s Идентификатор пользователя: '%d'", message, userId);
                    logger.debug(logMessage);
                    // Откатываем изменения до savepoint, чтобы отменить частично выполненные изменения
                    connection.rollback(rollbackPoint);
                    return;
                }
                int reward = task.getReward();
                int oldGold = clan.getGold();
                int newGold = oldGold + reward;
                // Добавление золота казне клана

                try {
                    updateClanGold(connection, clanId, newGold);
                } catch (SQLException e) {
                    connection.rollback(rollbackPoint);
                    throw e;
                }


                String message = String.format("Задание успешно выполнено пользователем. Пользователь: '%d'," +
                                " Задание: '%s', Награда: '%d', Текущее количество золота в казне: '%d'",
                        userId, task.getName(), reward, newGold);
                System.out.println(message);
                String logMessage = String.format("%s, Предыдущее количество золота в казне: '%d'", message, oldGold);
                logger.debug(logMessage);
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

    private void updateUserGold(Connection connection, long userId, int newGold) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_GOLD_QUERY);
        preparedStatement.setInt(1, newGold);
        preparedStatement.setLong(2, userId);
        preparedStatement.executeUpdate();
    }

    private Clan getClan(Connection connection, long clanId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            preparedStatement.setLong(1, clanId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Clan clan = new Clan();
                clan.setId(resultSet.getLong("id"));
                clan.setName(resultSet.getString("name"));
                clan.setGold(resultSet.getInt("gold"));
                return clan;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return null;

    }

}
