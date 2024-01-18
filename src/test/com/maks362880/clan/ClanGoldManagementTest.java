package com.maks362880.clan;

import com.maks362880.clan.model.Clan;
import com.maks362880.clan.model.Task;
import com.maks362880.clan.model.Users;
import com.maks362880.clan.service.ClanGoldManagement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.*;

import static com.maks362880.clan.service.ClanGoldManagement.*;
import static org.junit.Assert.assertEquals;


public class ClanGoldManagementTest {
    private Clan clan1;
    private Clan clan2;
    private Task task1;
    private Task task2;
    private Users users1;
    private Users users2;
    private ClanGoldManagement clanGoldManagement;
    private Connection connection;

    @Before
    public void setUp() {
        // Создание объектов для тестирования
        clan1 = new Clan("Clan 1", 10);
        clan2 = new Clan("Clan 2", 20);
        task1 = new Task("Task 1", 5);
        task2 = new Task("Task 2", 8);
        users1 = new Users("User 1", 15);
        users2 = new Users("User 2", 25);
        clanGoldManagement = new ClanGoldManagement();
        connection = clanGoldManagement.getConnection();
        // Запись объектов в базу данных
        try (Statement statement = connection.createStatement()) {
            // Создание таблицы кланов
            statement.executeUpdate(CREATE_CLAN_TABLE_QUERY);
            // Создание таблицы пользователей
            statement.executeUpdate(CREATE_USER_TABLE_QUERY);
            // Создание таблицы задач
            statement.executeUpdate(CREATE_TASKS_TABLE_QUERY);

            //записываем в бд данные получая обратно id
            clan1.setId(insertData(connection, clan1));
            clan2.setId(insertData(connection, clan2));
            users1.setId(insertData(connection, users1));
            users2.setId(insertData(connection, users2));
            task1.setId(insertData(connection, task1));
            task2.setId(insertData(connection, task2));
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public <T> int insertData(Connection connection, T data) {
        try {
            Class<?> clazz = data.getClass();
            String tableName = clazz.getSimpleName().toLowerCase();
            Field[] fields = clazz.getDeclaredFields();
            StringBuilder sb = new StringBuilder();
            StringBuilder valuePlaceholders = new StringBuilder();

            // Формирование SQL-запроса
            sb.append("INSERT INTO ").append(tableName).append(" (");
            for (Field field : fields) {
                if (!field.getName().equalsIgnoreCase("id")) { // Исключение поля Id
                    field.setAccessible(true);
                    sb.append(field.getName()).append(", ");
                    valuePlaceholders.append("?, ");
                }
            }
            sb.setLength(sb.length() - 2); // Удаление последней запятой
            valuePlaceholders.setLength(valuePlaceholders.length() - 2); // Удаление последней запятой
            sb.append(") VALUES (").append(valuePlaceholders).append(")");

            // Подготовка и выполнение запроса
            try (PreparedStatement statement = connection.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int parameterIndex = 1;
                for (Field field : fields) {
                    if (!field.getName().equalsIgnoreCase("id")) { // Исключение поля Id
                        field.setAccessible(true);
                        Object value = field.get(data);
                        statement.setObject(parameterIndex++, value);
                    }
                }
                statement.executeUpdate();

                // Получение сгенерированных ключей (если необходимо)
                ResultSet generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (IllegalAccessException | SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }


    @Test
    public void transferGoldFromUserToClan() throws SQLException {
        // Перевод золота от пользователя к клану
        clanGoldManagement.transferGoldFromUserToClan(connection, users1.getId(), clan1.getId(), 10);
        // Получение актуальной информации о золоте после перевода из базы данных
        users1.setGold(clanGoldManagement.getUserGoldAmount(connection, users1.getId()));
        clan1.setGold(clanGoldManagement.getClanGold(connection, clan1.getId()));
        // Проверка результатов
        assertEquals(5, users1.getGold());
        assertEquals(20, clan1.getGold());
    }

    @Test
    public void transferGoldFromTaskToClan() throws SQLException {
        // Перевод золота с выполненной задачи пользователем к клану
        clanGoldManagement.transferGoldFromUserTaskToClan(connection, users2.getId(), clan2.getId(), task2.getId());
        // Получение актуальной информации о золоте после перевода из базы данных
        users2.setGold(clanGoldManagement.getUserGoldAmount(connection, users2.getId()));
        clan2.setGold(clanGoldManagement.getClanGold(connection, clan2.getId()));
        // Проверка результатов
        assertEquals(25, users2.getGold());
        assertEquals(28, clan2.getGold());
    }

    @Test
    public void transferGoldBetweenClans() throws SQLException {
        // Перевод золота между кланами
        clanGoldManagement.transferGoldFromClanToClan(connection, clan2.getId(), clan1.getId(), 9);
        // Получение актуальной информации о золоте после перевода из базы данных
        clan1.setGold(clanGoldManagement.getClanGold(connection, clan1.getId()));
        clan2.setGold(clanGoldManagement.getClanGold(connection, clan2.getId()));
        // Проверка результатов
        assertEquals(19, clan1.getGold());
        assertEquals(11, clan2.getGold());
    }


    @After
    public void cleanup() {
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