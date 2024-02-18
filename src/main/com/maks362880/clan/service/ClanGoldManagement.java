package com.maks362880.clan.service;

import com.maks362880.clan.connectionpool.ConnectionPool;
import com.maks362880.clan.dao.ClanDao;
import com.maks362880.clan.dao.TaskDao;
import com.maks362880.clan.dao.UserDao;
import com.maks362880.clan.model.Clan;
import com.maks362880.clan.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.CompletableFuture;


public class ClanGoldManagement {
    private static final Logger logger = LoggerFactory.getLogger(ClanGoldManagement.class);

    private final ConnectionPool connectionPool;

    private final UserDao userDao;
    private final ClanDao clanDao;
    private final TaskDao taskDao;

    public ClanGoldManagement(ConnectionPool connectionPool, UserDao userDao, ClanDao clanDao, TaskDao taskDao) {
        this.connectionPool = connectionPool;
        this.userDao = userDao;
        this.clanDao = clanDao;
        this.taskDao = taskDao;
    }

    public CompletableFuture<Void> transferGoldFromUserToClan(long userId, long clanId, int gold) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = connectionPool.getConnection()) {
                // Проверяем, достаточно ли золота у пользователя
                int userGoldAmount = userDao.getUserGoldAmount(connection, userId);
                if (userGoldAmount < gold) {
                    String message = "У пользователя недостаточно золота.";
                    logger.debug("{} ID '{}'", message, userId);
                    return;
                }

                // Проверяем существование пользователя и клана
                if (!userDao.userExists(connection, userId) || !clanDao.clanExists(connection, clanId)) {
                    String message = "Пользователь или клан не существуют.";
                    logger.debug(message);
                    return;
                }

                // Формируем транзакцию
                connection.setAutoCommit(false);
                Savepoint rollbackPoint = null;
                try {
                    // Устанавливаем сохранную точку
                    rollbackPoint = connection.setSavepoint();

                    // Обновляем золото пользователя и золото клана
                    int newUserGoldAmount = userGoldAmount - gold;
                    userDao.updateUserGoldAmount(connection, userId, newUserGoldAmount);
                    int oldClanGold = clanDao.getClanGold(connection, clanId);
                    int newClanGold = oldClanGold + gold;
                    clanDao.updateClanGold(connection, clanId, newClanGold);

                    String message = String.format("Золото успешно добавлено в казну клана. Клан: '%s'," +
                                    " Пользователь: '%d', Добавленное золото: '%d', Текущее количество золота в казне: '%d' Предыдущее количество золота в казне: '%d'",
                            clanDao.getClanName(connection, clanId), userId, gold, newClanGold, oldClanGold);
                    logger.debug(message);

                    connection.commit();
                } catch (SQLException e) {
                    if (rollbackPoint != null) {
                        connection.rollback(rollbackPoint);
                    }
                    logger.error("Ошибка при добавлении золота в казну клана: {}", e.getMessage());
                } finally {
                    connection.setAutoCommit(true);
                    if (rollbackPoint != null) {
                        connection.releaseSavepoint(rollbackPoint);
                    }
                }
            } catch (SQLException e) {
                logger.error("Ошибка при получении соединения с базой данных: {}", e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> transferGoldFromUserTaskToClan(long userId, long clanId, long taskId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = connectionPool.getConnection()) {
                connection.setAutoCommit(false);
                Savepoint rollbackPoint = connection.setSavepoint();

                Clan clan = clanDao.getClan(connection, clanId);
                if (clan == null) {
                    connection.rollback(rollbackPoint);
                    String message = String.format("Клан с идентификатором '%d' не найден.", clanId);
                    System.out.println(message);
                    String logMessage = String.format("%s Идентификатор пользователя: '%d'", message, userId);
                    logger.debug(logMessage);
                    return;
                }

                Task task = taskDao.getTask(connection, taskId);
                if (task == null) {
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

                clanDao.updateClanGold(connection, clanId, newGold);

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
            try (Connection connection = connectionPool.getConnection()) {
                // Получение текущего золота источникового клана из базы данных
                int sourceClanGold = clanDao.getClanGold(connection, sourceClanId);

                // Получение текущего золота целевого клана из базы данных
                int targetClanGold = clanDao.getClanGold(connection, targetClanId);

                // Проверка достаточности золота у источникового клана
                if (sourceClanGold >= amount) {
                    // Обновление золота источникового и целевого кланов в базе данных
                    clanDao.updateClanGold(connection, sourceClanId, sourceClanGold - amount);
                    clanDao.updateClanGold(connection, targetClanId, targetClanGold + amount);

                    logger.info("Успешная передача {} золота от клана {} к клану {}. Текущий баланс {}. Предыдущее количество золота в клане {}",
                            amount, sourceClanId, targetClanId, targetClanGold + amount, targetClanGold);
                } else {
                    logger.warn("Недостаточно золота. Передающий золото клан {} имеет только {} золота",
                            sourceClanId, sourceClanGold);
                }
            } catch (SQLException e) {
                logger.error("Error occurred while transferring gold from clan to clan", e);
            }
        });
    }

}
