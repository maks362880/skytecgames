package com.maks362880.clan.dao;

import com.maks362880.clan.model.Task;

import java.sql.Connection;

public interface TaskDao {

    Task getTask(Connection connection, long taskId);
}
