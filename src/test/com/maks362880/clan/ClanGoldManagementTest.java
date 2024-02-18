package com.maks362880.clan;

import com.maks362880.clan.connectionpool.ConnectionPool;
import com.maks362880.clan.dao.*;
import com.maks362880.clan.dbhelper.DbHelper;
import com.maks362880.clan.model.Clan;
import com.maks362880.clan.model.Task;
import com.maks362880.clan.model.Users;
import com.maks362880.clan.service.ClanGoldManagement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClanGoldManagementTest {

    private static Clan clan1;
    private static Clan clan2;
    private static Task task1;
    private static Task task2;
    private static Users users1;
    private static Users users2;
    private static ClanGoldManagement clanGoldManagement;

    static ConnectionPool connectionPool;
    static UserDao userDao;
    static ClanDao clanDao;
    static TaskDao taskDao;

    @BeforeEach
    public void setUp() {
        connectionPool = new ConnectionPool();
        userDao = new UserDaoImpl();
        clanDao = new ClanDaoImpl();
        taskDao = new TaskDaoImpl();
        clanGoldManagement = new ClanGoldManagement(connectionPool, userDao, clanDao, taskDao);

        try (Connection connection = connectionPool.getConnection()) {
            DbHelper.init(connection);
            clan1 = DbHelper.clan1;
            clan2 = DbHelper.clan2;
            users1 = DbHelper.users1;
            users2 = DbHelper.users2;
            task1 = DbHelper.task1;
            task2 = DbHelper.task2;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void transferGoldFromUserToClan() {
        AtomicReference<Integer> actualUserGoldAmount = new AtomicReference<>();
        AtomicReference<Integer> actualClanGold = new AtomicReference<>();

        CompletableFuture<Void> transferFuture = clanGoldManagement.transferGoldFromUserToClan(users1.getId(), clan1.getId(), 10)
                .thenRunAsync(() -> {
                    try (Connection connection = connectionPool.getConnection()) {
                        actualUserGoldAmount.set(userDao.getUserGoldAmount(connection, users1.getId()));
                        actualClanGold.set(clanDao.getClanGold(connection, clan1.getId()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        transferFuture.join();
        assertEquals(5, actualUserGoldAmount.get());
        assertEquals(20, actualClanGold.get());
    }

    @Test
    public void transferGoldFromTaskToClan() {
        AtomicInteger actualUserGoldAmount = new AtomicInteger();
        AtomicInteger actualClanGold = new AtomicInteger();

        CompletableFuture<Void> transferFuture = clanGoldManagement.transferGoldFromUserTaskToClan(users2.getId(), clan2.getId(), task2.getId())
                .thenRunAsync(() -> {
                    try (Connection connection = connectionPool.getConnection()) {
                        actualUserGoldAmount.set(userDao.getUserGoldAmount(connection, users2.getId()));
                        actualClanGold.set(clanDao.getClanGold(connection, clan2.getId()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        transferFuture.join();
        assertEquals(25, actualUserGoldAmount.get());
        assertEquals(28, actualClanGold.get());
    }

    @Test
    public void transferGoldBetweenClans() {
        AtomicInteger actualSourceClanGold = new AtomicInteger();
        AtomicInteger actualTargetClanGold = new AtomicInteger();

        CompletableFuture<Void> transferFuture = clanGoldManagement.transferGoldFromClanToClan(clan2.getId(), clan1.getId(), 9)
                .thenRunAsync(() -> {
                    try (Connection connection = connectionPool.getConnection()) {
                        actualSourceClanGold.set(clanDao.getClanGold(connection, clan2.getId()));
                        actualTargetClanGold.set(clanDao.getClanGold(connection, clan1.getId()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        transferFuture.join();
        assertEquals(11, actualSourceClanGold.get());
        assertEquals(19, actualTargetClanGold.get());
    }

    @AfterEach
    public void cleanup() {
        try (Connection connection = connectionPool.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS clan");
            statement.executeUpdate("DROP TABLE IF EXISTS users");
            statement.executeUpdate("DROP TABLE IF EXISTS task");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}