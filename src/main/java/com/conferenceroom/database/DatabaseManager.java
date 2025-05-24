package com.conferenceroom.database;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:conference_room.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON;");
        return conn;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create rooms table
            String createRoomsTable = """
                CREATE TABLE IF NOT EXISTS rooms (
                    room_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    capacity INTEGER NOT NULL,
                    location TEXT NOT NULL,
                    price_per_hour REAL NOT NULL
                )""";
            stmt.execute(createRoomsTable);

            // Migrate old rooms table if it exists with room_name or image_url
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "rooms", "room_name")) {
                if (rs.next()) {
                    stmt.execute("ALTER TABLE rooms RENAME COLUMN room_name TO name");
                    stmt.execute("ALTER TABLE rooms ADD COLUMN location TEXT NOT NULL DEFAULT 'Unknown'");
                    stmt.execute("ALTER TABLE rooms ADD COLUMN price_per_hour REAL NOT NULL DEFAULT 50.0");
                }
            }
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "rooms", "image_url")) {
                if (rs.next()) {
                    stmt.execute("ALTER TABLE rooms DROP COLUMN image_url");
                }
            }

            // Create users table
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'USER'))
                )""";
            stmt.execute(createUsersTable);

            // Drop and recreate reservations table to ensure fresh schema
            stmt.execute("DROP TABLE IF EXISTS reservations");
            String createReservationsTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL,
                    room_id INTEGER NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    has_equipment INTEGER NOT NULL CHECK(has_equipment IN (0, 1)),
                    total_cost REAL NOT NULL,
                    FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE,
                    FOREIGN KEY (room_id) REFERENCES rooms (room_id) ON DELETE CASCADE
                )""";
            stmt.execute(createReservationsTable);

            // Insert sample rooms
            String insertRooms = """
                INSERT OR IGNORE INTO rooms (name, capacity, location, price_per_hour) VALUES
                ('Conference Room A', 10, 'Building 1, Floor 2', 50.0),
                ('Board Room B', 20, 'Building 2, Floor 1', 75.0),
                ('Meeting Room C', 15, 'Building 1, Floor 3', 60.0)""";
            stmt.execute(insertRooms);

            // Insert default admin user
            String hashedPassword = BCrypt.hashpw("admin", BCrypt.gensalt());
            String insertAdmin = """
                INSERT OR IGNORE INTO users (username, password, role) VALUES
                ('admin', ?, 'ADMIN')""";
            try (PreparedStatement pstmt = conn.prepareStatement(insertAdmin)) {
                pstmt.setString(1, hashedPassword);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
