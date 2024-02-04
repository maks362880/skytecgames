package com.maks362880.clan.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDaoImpl implements UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    public static final String SELECT_USER_GOLD_QUERY = "SELECT gold FROM users WHERE id = ?";
    private static final String UPDATE_USER_GOLD_QUERY = "UPDATE users SET gold = ? WHERE id = ?";
    private static final String SELECT_USER_QUERY = "SELECT * FROM users WHERE id = ?";

    @Override
    public void updateUserGold(Connection connection, long userId, int newGold) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_USER_GOLD_QUERY)) {
            statement.setInt(1, newGold);
            statement.setLong(2, userId);
            statement.executeUpdate();
            logger.info("Успешно обновлено золото пользователя с ID '{}'", userId);
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении золота пользователя с ID '{}': {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getUserGoldAmount(Connection connection, long userId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_USER_GOLD_QUERY)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("gold");
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении количества золота пользователя с ID '{}': {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
        return 0;
    }

    @Override
    public void updateUserGoldAmount(Connection connection, long userId, int newGoldAmount) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_USER_GOLD_QUERY)) {
            statement.setInt(1, newGoldAmount);
            statement.setLong(2, userId);
            statement.executeUpdate();
            logger.info("Успешно обновлено количество золота пользователя с ID '{}'", userId);
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении количества золота пользователя с ID '{}': {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean userExists(Connection connection, long userId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_USER_QUERY)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке существования пользователя с ID '{}': {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
