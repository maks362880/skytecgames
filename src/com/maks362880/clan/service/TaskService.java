public class TaskService { // какой-то сервис с заданиями

    private final ClanService clans;

    public TaskService(ClanService clans) {
        this.clans = clans;
    }

    void completeTask(long clanId, long taskId) {
        // ...

        // if (success)
        {
            Clan clan = clans.getClan(clanId);
            // clan.[gold] += gold;
            // как-то сохранить изменения
        }
    }
}