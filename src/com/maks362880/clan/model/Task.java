import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id && reward == task.reward && Objects.equals(name, task.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, reward);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", reward=" + reward +
                '}';
    }
}
