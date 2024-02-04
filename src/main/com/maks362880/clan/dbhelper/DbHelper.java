package com.maks362880.clan.dbhelper;

import com.maks362880.clan.model.Clan;
import com.maks362880.clan.model.Task;
import com.maks362880.clan.model.Users;

import java.lang.reflect.Field;
import java.sql.*;

public class DbHelper {
    public static Clan clan1;
    public static Clan clan2;
    public static Task task1;
    public static Task task2;
    public static Users users1;
    public static Users users2;
    private static Connection connection;

    public static final String CREATE_CLAN_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS clan (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), gold INT)";
    public static final String CREATE_USER_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), gold INT)";
    public static final String CREATE_TASKS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS task (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), reward INT)";

    public static void init(Connection connection) {
        DbHelper.connection = connection;
        createEntities();
        createTables(DbHelper.connection);
        fillData();
    }

    public static void createEntities() {
        clan1 = new Clan("Clan 1", 10);
        clan2 = new Clan("Clan 2", 20);
        task1 = new Task("Task 1", 5);
        task2 = new Task("Task 2", 8);
        users1 = new Users("User 1", 15);
        users2 = new Users("User 2", 25);
    }

    public static void createTables(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            // Создание таблицы кланов
            statement.executeUpdate(CREATE_CLAN_TABLE_QUERY);
            // Создание таблицы пользователей
            statement.executeUpdate(CREATE_USER_TABLE_QUERY);
            // Создание таблицы задач
            statement.executeUpdate(CREATE_TASKS_TABLE_QUERY);
            //записываем в бд данные получая обратно id
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void fillData() {
        clan1.setId(insertData(connection, clan1));
        clan2.setId(insertData(connection, clan2));
        users1.setId(insertData(connection, users1));
        users2.setId(insertData(connection, users2));
        task1.setId(insertData(connection, task1));
        task2.setId(insertData(connection, task2));
    }

    public static <T> int insertData(Connection connection, T data) {
        try {
            Class<?> clazz = data.getClass();
            String tableName = clazz.getSimpleName().toLowerCase();
            Field[] fields = clazz.getDeclaredFields();
            StringBuilder sb = new StringBuilder();
            StringBuilder valuePlaceholders = new StringBuilder();

            // Формирование SQL-запроса
            sb.append("INSERT INTO ").append(tableName).append(" (");
            for (Field field : fields) {
                // Исключение поля Id
                if (!field.getName().equalsIgnoreCase("id")) {
                    field.setAccessible(true);
                    sb.append(field.getName()).append(", ");
                    valuePlaceholders.append("?, ");
                }
            }

            // Удаление последней запятой
            sb.setLength(sb.length() - 2);
            // Удаление последней запятой
            valuePlaceholders.setLength(valuePlaceholders.length() - 2);
            sb.append(") VALUES (").append(valuePlaceholders).append(")");

            // Подготовка и выполнение запроса
            try (PreparedStatement statement = connection.prepareStatement(sb.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int parameterIndex = 1;
                for (Field field : fields) {
                    // Исключение поля Id
                    if (!field.getName().equalsIgnoreCase("id")) {
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

}
