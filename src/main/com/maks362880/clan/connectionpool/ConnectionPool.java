package com.maks362880.clan.connectionpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    private static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String DB_USERNAME = "skytecgames";
    private static final String DB_PASSWORD = "skytecgames";
    private static final int INITIAL_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private final BlockingQueue<Connection> connections;

    public ConnectionPool() {
        connections = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
        initializeConnectionPool();
    }

    private void initializeConnectionPool() {
        try {
            Class.forName("org.h2.Driver");
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                connections.add(createNewConnection());
            }
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load H2 driver: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Connection createNewConnection() {
        try {
            return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (SQLException e) {
            logger.error("Failed to create a new connection: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        try {
            return connections.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Error getting connection: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void releaseConnection(Connection connection) {
        try {
            if (connection.isValid(5)) {
                connections.offer(connection);
            } else {
                logger.warn("Attempting to release an invalid connection, closing it instead");
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Error releasing connection: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}