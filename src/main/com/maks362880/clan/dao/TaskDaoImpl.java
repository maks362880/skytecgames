package com.maks362880.clan.dao;

import com.maks362880.clan.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TaskDaoImpl implements TaskDao {
    private static final Logger logger = LoggerFactory.getLogger(TaskDaoImpl.class);
    private static final String SELECT_TASK_QUERY = "SELECT * FROM task WHERE id = ?";

    @Override
    public Task getTask(Connection connection, long taskId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_TASK_QUERY)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String name = resultSet.getString("name");
                    int reward = resultSet.getInt("reward");
                    Task task = new Task();
                    task.setId(id);
                    task.setReward(reward);
                    task.setName(name);
                    return task;
                }
            }
        } catch (SQLException e) {
            logger.error("Не удается получить задачу с ID '{}': {}", taskId, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
