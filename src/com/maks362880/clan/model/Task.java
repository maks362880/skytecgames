public class Task {
    private long id;
    private String name;
    private int reward;

    public Task(long id, String name, int reward) {
        this.id = id;
        this.name = name;
        this.reward = reward;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getReward() {
        return reward;
    }
}
