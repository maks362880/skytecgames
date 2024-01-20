package com.maks362880.clan.dbhelper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.maks362880.clan.service.ClanGoldManagement.*;

public class ConnectionHelper {
    private static Connection connection;

    private ConnectionHelper() {
    }

    public static synchronized Connection getConnection() {
        if (connection == null || isClosed(connection)) {
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return connection;
    }

    private static boolean isClosed(Connection conn) {
        try {
            return conn.isClosed();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
