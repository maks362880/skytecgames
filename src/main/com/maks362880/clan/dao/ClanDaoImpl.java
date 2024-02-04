package com.maks362880.clan.dao;

import com.maks362880.clan.model.Clan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClanDaoImpl implements ClanDao {

    private static final Logger logger = LoggerFactory.getLogger(ClanDaoImpl.class);

    public static final String SELECT_CLAN_QUERY = "SELECT * FROM clan WHERE id = ?";
    private static final String UPDATE_CLAN_GOLD_QUERY = "UPDATE clan SET gold = ? WHERE id = ?";

    @Override
    public void updateClanGold(Connection connection, long clanId, int goldAdded) {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_CLAN_GOLD_QUERY)) {
            statement.setInt(1, goldAdded);
            statement.setLong(2, clanId);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка при обновлении золота клана с ID '{}': {}", clanId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Clan getClan(Connection connection, long clanId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    Clan clan = new Clan();
                    clan.setId(result.getLong("id"));
                    clan.setName(result.getString("name"));
                    clan.setGold(result.getInt("gold"));
                    return clan;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении информации о клане с ID '{}': {}", clanId, e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public String getClanName(Connection connection, long clanId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getString("name");
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении названия клана с ID '{}': {}", clanId, e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public boolean clanExists(Connection connection, long clanId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            logger.error("Ошибка при проверке существования клана с ID '{}': {}", clanId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getClanGold(Connection connection, long clanId) {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_CLAN_QUERY)) {
            statement.setLong(1, clanId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt("gold");
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении количества золота клана с ID '{}': {}", clanId, e.getMessage());
            throw new RuntimeException(e);
        }
        return 0;
    }

}
