import enums.UserTransactionsReasons;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ClanGoldManagement clanGoldManagement = new ClanGoldManagement();


        // Ввод данных с консоли
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите ID пользователя: ");
        long userId = scanner.nextLong();
        System.out.print("Введите ID клана: ");
        long clanId = scanner.nextLong();
        System.out.print("Введите количество золота: ");
        int gold = scanner.nextInt();
        System.out.print("Введите ID выполненной задачи: ");
        long taskId = scanner.nextLong();

        // Пример использования
        clanGoldManagement.addGoldToClan(userId, clanId, gold, UserTransactionsReasons.USER_CONTRIBUTION);
        clanGoldManagement.completeTask(userId,clanId, taskId);
    }
}