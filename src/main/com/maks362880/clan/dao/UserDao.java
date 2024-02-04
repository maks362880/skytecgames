package com.maks362880.clan.dao;

import java.sql.Connection;

public interface UserDao {

    void updateUserGold(Connection connection, long userId, int newGold);

    int getUserGoldAmount(Connection connection, long userId);

    void updateUserGoldAmount(Connection connection, long userId, int newGoldAmount);

    boolean userExists(Connection connection, long userId);
}
