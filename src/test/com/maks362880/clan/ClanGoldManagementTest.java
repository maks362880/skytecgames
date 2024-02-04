package com.maks362880.clan;

import com.maks362880.clan.connectionpool.ConnectionPool;
import com.maks362880.clan.dao.*;
import com.maks362880.clan.dbhelper.DbHelper;
import com.maks362880.clan.model.Clan;
import com.maks362880.clan.model.Task;
import com.maks362880.clan.model.Users;
import com.maks362880.clan.service.ClanGoldManagement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ClanGoldManagementTest {
    private static Clan clan1;
    private static Clan clan2;
    private static Task task1;
    private static Task task2;
    private static Users users1;
    private static Users users2;
    private static ClanGoldManagement clanGoldManagement;

    static Connection connection;
    static UserDao userDao;
    static ClanDao clanDao;
    static TaskDao taskDao;

    @BeforeAll
    public static void setUp() {
        ConnectionPool connectionPool = new ConnectionPool();
        connection = connectionPool.getConnection();
        userDao = new UserDaoImpl();
        clanDao = new ClanDaoImpl();
        taskDao = new TaskDaoImpl();
        clanGoldManagement = new ClanGoldManagement(connectionPool, userDao, clanDao, taskDao);
        DbHelper.init(connection);
        clan1 = DbHelper.clan1;
        clan2 = DbHelper.clan2;
        users1 = DbHelper.users1;
        users2 = DbHelper.users2;
        task1 = DbHelper.task1;
        task2 = DbHelper.task2;
    }

    @Test
    public void transferGoldFromUserToClan() {
        // Перевод золота от пользователя к клану
        clanGoldManagement.transferGoldFromUserToClan(users1.getId(), clan1.getId(), 10)
                .join();
        // Получение актуальной информации о золоте после перевода из базы данных
        int actualUserGoldAmount = userDao.getUserGoldAmount(connection, users1.getId());
        int actualClanGold = clanDao.getClanGold(connection, clan1.getId());
        // Проверка результатов
        assertEquals(5, actualUserGoldAmount);
        assertEquals(20, actualClanGold);
    }

    @Test
    public void transferGoldFromTaskToClan() {
        // Перевод золота с выполненной задачи пользователем к клану
        clanGoldManagement.transferGoldFromUserTaskToClan(users2.getId(), clan2.getId(), task2.getId())
                .join();
        // Получение актуальной информации о золоте после перевода из базы данных
        int actualUserGoldAmount = userDao.getUserGoldAmount(connection, users2.getId());
        int actualClanGold = clanDao.getClanGold(connection, clan2.getId());
        // Проверка результатов
        assertEquals(25, actualUserGoldAmount);
        assertEquals(28, actualClanGold);
    }

    @Test
    public void transferGoldBetweenClans() {
        // Перевод золота между кланами
        clanGoldManagement.transferGoldFromClanToClan(clan2.getId(), clan1.getId(), 9)
                .join();
        // Получение актуальной информации о золоте после перевода из базы данных
        int actualSourceClanGold = clanDao.getClanGold(connection, clan2.getId());
        int actualTargetClanGold = clanDao.getClanGold(connection, clan1.getId());
        // Проверка результатов
        assertEquals(11, actualSourceClanGold);
        assertEquals(19, actualTargetClanGold);
    }


    @AfterAll
    public static void cleanup() {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS clan");
            statement.executeUpdate("DROP TABLE IF EXISTS users");
            statement.executeUpdate("DROP TABLE IF EXISTS task");
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}