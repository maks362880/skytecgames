package com.maks362880.clan.service;

import com.maks362880.clan.dbhelper.ConnectionHelper;
import com.maks362880.clan.model.Clan;
import com.maks362880.clan.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.concurrent.CompletableFuture;


public class ClanGoldManagement {
    public static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    public static final String DB_USERNAME = "skytecgames";
    public static final String DB_PASSWORD = "skytecgames";
    public static final String CREATE_CLAN_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS clan (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), gold INT)";
    public static final String CREATE_USER_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), gold INT)";
    public static final String CREATE_TASKS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS task (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), reward INT)";
    public static final String SELECT_CLAN_QUERY = "SELECT * FROM clan WHERE id = ?";
    public static final String SELECT_USER_GOLD_QUERY = "SELECT gold FROM users WHERE id = ?";
    private static final String UPDATE_USER_GOLD_QUERY = "UPDATE users SET gold = ? WHERE id = ?";
    private static final String SELECT_USER_QUERY = "SELECT * FROM users WHERE id = ?";
    private static final String UPDATE_CLAN_GOLD_QUERY = "UPDATE clan SET gold = ? WHERE id = ?";
    private static final String SELECT_TASK_QUERY = "SELECT * FROM task WHERE id = ?";
    private static final Logger logger = LoggerFactory.getLogger(ClanGoldManagement.class);

    private final Connection connection;

    public ClanGoldManagement() {
        this.connection = ConnectionHelper.getConnection();
    }

    public CompletableFuture<Void> transferGoldFromUserToClan(long userId, long clanId, int gold) {
        return CompletableFuture.runAsync(() -> {
             try {
                // Формируем точку отката только для текущего потока
                Savepoint rollbackPoint = null;

                try {
                    // Проверяем, достаточно ли золота у пользователя
                    int userGoldAmount = getUserGoldAmount(userId);
                    if (userGoldAmount < gold) {
                        String message = "У пользователя недостаточно золота.";
                        logger.debug("{} ID '{}'", message, userId);
                        return;
                    }

                    // Проверяем существование пользователя и клана
                    if (!userExists(userId) || !clanExists(clanId)) {
                        String message = "Пользователь или клан не существуют.";
                        logger.debug(message);
                        return;
                    }

                    // Устанавливаем сохранную точку только для текущего потока
                    rollbackPoint = connection.setSavepoint();

                    // Обновляем золото пользователя и золото клана
                    int newUserGoldAmount = userGoldAmount - gold;
                    updateUserGoldAmount(userId, newUserGoldAmount);
                    int oldClanGold = getClanGold(clanId);
                    int newClanGold = oldClanGold + gold;
                    updateClanGold(clanId, newClanGold);

                    String message = String.format("Золото успешно добавлено в казну клана. Клан: '%s'," +
                                    " Пользователь: '%d', Добавленное золото: '%d', Текущее количество золота в казне: '%d' Предыдущее количество золота в казне: '%d'",
                            getClanName(clanId), userId, gold, newClanGold, oldClanGold);
                    logger.debug(message);

                    connection.commit(); // Подтверждаем транзакцию только после успешного выполнения задачи
                } catch (SQLException e) {
                    if (rollbackPoint != null) {
                        connection.rollback(rollbackPoint); // Откатываем изменения только для текущего потока
                    }
                    logger.error("Ошибка при добавлении золота в казну клана: {}", e.getMessage());
                } finally {
                    connection.releaseSavepoint(rollbackPoint); // Освобождаем используемую точку отката
                }
            } catch (SQLException e) {
                logger.error("Ошибка при получении соединения с базой данных: {}", e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> transferGoldFromUserTaskToClan(long userId, long clanId, long taskId) {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.setAutoCommit(false);
                Savepoint rollbackPoint = connection.setSavepoint();

                Clan clan = getClan(clanId);
                if (clan == null) {
                    connection.rollback(rollbackPoint);
                    String message = String.format("Клан с идентификатором '%d' не найден.", clanId);
                    System.out.println(message);
                    String logMessage = String.format("%s Идентификатор пользователя: '%d'", message, userId);
                    logger.debug(logMessage);
                    return;
                }

                Task task = getTask(taskId);
                if (task == null) {
                    // Откатываем изменения до savepoint, чтобы отменить частично выполненные изменения
                    connection.rollback(rollbackPoint);
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

                try {
                    updateClanGold(clanId, newGold);
                } catch (SQLException e) {
                    connection.rollback(rollbackPoint);
                    throw e;
                }

                String message = String.format("Задание успешно выполнено. Пользователь: '%d'," +
                                " Задание: '%s', Награда: '%d', Клан: '%d', Текущее количество золота в казне клана: '%d'",
                        userId, task.getName(), reward, clanId, newGold);
                String logMessage = String.format("%s, Предыдущее количество золота в казне: '%d'", message, oldGold);
                logger.debug(logMessage);
                connection.commit();
            } catch (SQLException e) {
                logger.error("Ошибка при выполнении задачи: {}", e.getMessage());
            }
        });
    }


    public CompletableFuture<Void> transferGoldFromClanToClan(long sourceClanId, long targetClanId, int amount) {
        return CompletableFuture.runAsync(() -> {
             try {
                // Получение текущего золота источникового и целевого кланов из базы данных
                PreparedStatement getSourceClanGoldStatement = connection.prepareStatement(SELECT_CLAN_QUERY);
                getSourceClanGoldStatement.setLong(1, sourceClanId);
                ResultSet sourceClanGoldResult = getSourceClanGoldStatement.executeQuery();
                int sourceClanGold = 0;
                if (sourceClanGoldResult.next()) {
                    sourceClanGold = sourceClanGoldResult.getInt("gold");
                }
                getSourceClanGoldStatement.close();

                PreparedStatement getTargetClanGoldStatement = connection.prepareStatement(SELECT_CLAN_QUERY);
                getTargetClanGoldStatement.setLong(1, targetClanId);
                ResultSet targetClanGoldResult = getTargetClanGoldStatement.executeQuery();
                int targetClanGold = 0;
                if (targetClanGoldResult.next()) {
                    targetClanGold = targetClanGoldResult.getInt("gold");
                }
                getTargetClanGoldStatement.close();

                // Проверка достаточности золота у источникового клана
                if (sourceClanGold >= amount) {
                    // Обновление золота источникового и целевого кланов в базе данных
                    PreparedStatement updateSourceClanGoldStatement = connection.prepareStatement(UPDATE_CLAN_GOLD_QUERY);
                    updateSourceClanGoldStatement.setInt(1, sourceClanGold - amount);
                    updateSourceClanGoldStatement.setLong(2, sourceClanId);
                    updateSourceClanGoldStatement.executeUpdate();
                    updateSourceClanGoldStatement.close();

                    PreparedStatement updateTargetClanGoldStatement = connection.prepareStatement(UPDATE_CLAN_GOLD_QUERY);
                    updateTargetClanGoldStatement.setInt(1, targetClanGold + amount);
                    updateTargetClanGoldStatement.setLong(2, targetClanId);
                    updateTargetClanGoldStatement.executeUpdate();
                    updateTargetClanGoldStatement.close();

                    logger.info("Успешная передача {} золота от клана {} к клану {}. Текущий баланс {}. Предыдущее количество золота в клане {}",
                            amount, sourceClanId, targetClanId, targetClanGold + amount, targetClanGold);
                } else {
                    logger.warn("Недостаточно золота. Передающий золото клан  {} имеет только {} золота",
                            sourceClanId, sourceClanGold);
                }
                // Автоматически подтверждаем транзакцию
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                logger.error("Error occurred while transferring gold from clan to clan", e);
            }
        });
    }


    private Task getTask(long taskId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_TASK_QUERY)) {
            statement.setLong(1, taskId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                long id = resultSet.getLong("id");
                String name = resultSet.getString("name");
                int reward = resultSet.getInt("reward");
                Task task = new Task();
                task.setId(id);
                task.setReward(reward);
                task.setName(name);
                return task;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void updateUserGold(long userId, int newGold) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_GOLD_QUERY);
        preparedStatement.setInt(1, newGold);
        preparedStatement.setLong(2, userId);
        preparedStatement.executeUpdate();
    }

    private Clan getClan(long clanId) throws SQLException {
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

    public int getUserGoldAmount(long userId) throws SQLException {
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

    private void updateUserGoldAmount(long userId, int newGoldAmount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_USER_GOLD_QUERY)) {
            statement.setInt(1, newGoldAmount);
            statement.setLong(2, userId);
            statement.executeUpdate();
        }
    }

    private boolean userExists(long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_USER_QUERY)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean clanExists(long clanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private String getClanName(long clanId) throws SQLException {
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

    public int getClanGold(long clanId) throws SQLException {
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

    private void updateClanGold(long clanId, int goldAdded) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_CLAN_GOLD_QUERY)) {
            statement.setInt(1, goldAdded);
            statement.setLong(2, clanId);
            statement.executeUpdate();
        }
    }

}
