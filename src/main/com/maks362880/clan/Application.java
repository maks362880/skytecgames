package com.maks362880.clan;


public class Application {
    public static void main(String[] args) {
     //   ClanGoldManagement clanGoldManagement = new ClanGoldManagement();
//
//
//        // Ввод данных с консоли
//        Scanner scanner = new Scanner(System.in);
//        System.out.print("Введите ID пользователя: ");
//        long userId = scanner.nextLong();
//        System.out.print("Введите ID клана: ");
//        long clanId = scanner.nextLong();
//        System.out.print("Введите количество золота: ");
//        int gold = scanner.nextInt();
//        System.out.print("Введите ID выполненной задачи: ");
//        long taskId = scanner.nextLong();
//        Clan clan1 = new Clan(1, "Clan 1", 10);
//        Clan clan2 = new Clan(2, "Clan 2", 20);
//        Task task1 = new Task(1, "Task 1", 5);
//        Task task2 = new Task(2, "Task 2", 8);
//        User user1 = new User(1, "User 1", 15);
//        User user2 = new User(2, "User 2", 25);
//
//        Connection connection = null;
//        // Создание соединения с базой данных
//        try {
//            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        // Запись объектов в базу данных
//        try (Statement statement = connection.createStatement()) {
//            // Создание таблицы кланов
//            statement.executeUpdate(CREATE_CLAN_TABLE_QUERY);
//            // Создание таблицы пользователей
//            statement.executeUpdate(CREATE_USER_TABLE_QUERY);
//            // Создание таблицы задач
//            statement.executeUpdate(CREATE_TASKS_TABLE_QUERY);
//            // Добавление записей в таблицу кланов
//            statement.executeUpdate("INSERT INTO clan (id, name, gold) VALUES (" + clan1.getId()
//                    + ", '" + clan1.getName() + "', " + clan1.getGold() + ")");
//            statement.executeUpdate("INSERT INTO clan (id, name, gold) VALUES (" + clan2.getId()
//                    + ", '" + clan2.getName() + "', " + clan2.getGold() + ")");
//            // Добавление записей в таблицу пользователей
//            statement.executeUpdate("INSERT INTO users (id, name, gold) VALUES (" + user1.getId()
//                    + ", '" + user1.getName() + "', " + user1.getGold() + ")");
//            statement.executeUpdate("INSERT INTO users (id, name, gold) VALUES (" + user2.getId()
//                    + ", '" + user2.getName() + "', " + user2.getGold() + ")");
//            // Добавление записей в таблицу заданий
//            statement.executeUpdate("INSERT INTO task (id, name, reward) VALUES (" + task1.getId()
//                    + ", '" + task1.getName() + "', " + task1.getReward() + ")");
//            statement.executeUpdate("INSERT INTO task (id, name, reward) VALUES (" + task2.getId()
//                    + ", '" + task2.getName() + "', " + task2.getReward() + ")");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }


        // Пример использования
//        clanGoldManagement.transferGoldFromUserToClan(user1.getId(), clan1.getId(), 3);
//        clanGoldManagement.transferGoldFromUserTaskToClan(user2.getId(), clan2.getId(), task2.getId());
//        clanGoldManagement.transferGoldFromClanToClan(clan1.getId(),clan2.getId(),3);
    }
}