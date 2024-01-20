package com.maks362880.clan;

import com.maks362880.clan.dbhelper.ConnectionHelper;
import com.maks362880.clan.dbhelper.DbHelper;
import com.maks362880.clan.model.Clan;
import com.maks362880.clan.model.Task;
import com.maks362880.clan.model.Users;
import com.maks362880.clan.service.ClanGoldManagement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    @BeforeAll
    public static void setUp() {
        clanGoldManagement = new ClanGoldManagement();
        DbHelper.init();
        clan1 = DbHelper.clan1;
        clan2 = DbHelper.clan2;
        users1 = DbHelper.users1;
        users2 = DbHelper.users2;
        task1 = DbHelper.task1;
        task2 = DbHelper.task2;
    }


    @Test
    public void transferGoldFromUserToClan(){
        // Перевод золота от пользователя к клану
        clanGoldManagement.transferGoldFromUserToClan(users1.getId(), clan1.getId(), 10)
                .thenRunAsync(() -> {
            try {
        // Получение актуальной информации о золоте после перевода из базы данных
        users1.setGold(clanGoldManagement.getUserGoldAmount(users1.getId()));
        clan1.setGold(clanGoldManagement.getClanGold(clan1.getId()));
        // Проверка результатов
        assertEquals(5, users1.getGold());
        assertEquals(20, clan1.getGold());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
                });
    }

    @Test
    public void transferGoldFromTaskToClan() {
        // Перевод золота с выполненной задачи пользователем к клану
        clanGoldManagement.transferGoldFromUserTaskToClan(users2.getId(), clan2.getId(), task2.getId())
                .thenRunAsync(() -> {
            try {
        // Получение актуальной информации о золоте после перевода из базы данных
        users2.setGold(clanGoldManagement.getUserGoldAmount(users2.getId()));
        clan2.setGold(clanGoldManagement.getClanGold(clan2.getId()));
        // Проверка результатов
        assertEquals(25, users2.getGold());
        assertEquals(28, clan2.getGold());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
                });
    }

    @Test
    public void transferGoldBetweenClans() {
        // Перевод золота между кланами
        clanGoldManagement.transferGoldFromClanToClan(clan2.getId(), clan1.getId(), 9)
                .thenRunAsync(() -> {
                    try {
                        // Получение актуальной информации о золоте после перевода из базы данных
                        clan1.setGold(clanGoldManagement.getClanGold(clan1.getId()));
                        clan2.setGold(clanGoldManagement.getClanGold(clan2.getId()));
                        // Проверка результатов
                        assertEquals(19, clan1.getGold());
                        assertEquals(11, clan2.getGold());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    @AfterAll
    public static void cleanup() {
        try (Statement statement = ConnectionHelper.getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS clan");
            statement.executeUpdate("DROP TABLE IF EXISTS users");
            statement.executeUpdate("DROP TABLE IF EXISTS task");
        } catch (SQLException e) {
            try {
                ConnectionHelper.getConnection().close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


}