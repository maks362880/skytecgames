package com.maks362880.clan.dao;

import com.maks362880.clan.model.Clan;

import java.sql.Connection;

public interface ClanDao {

    void updateClanGold(Connection connection, long clanId, int goldAdded);

    int getClanGold(Connection connection, long clanId);

    String getClanName(Connection connection, long clanId);

    boolean clanExists(Connection connection, long clanId);

    Clan getClan(Connection connection, long clanId);


}
