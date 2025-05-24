package com.conferenceroom.database;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:conference_room.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            // Create rooms table
            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                         "room_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "name TEXT NOT NULL UNIQUE, " +
                         "capacity INTEGER NOT NULL, " +
                         "location TEXT NOT NULL, " +
                         "price_per_hour REAL NOT NULL)");

            // Create equipment table
            stmt.execute("CREATE TABLE IF NOT EXISTS equipment (" +
                         "equipment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "name TEXT NOT NULL UNIQUE, " +
                         "cost REAL NOT NULL)");

            // Create room_equipment table
            stmt.execute("CREATE TABLE IF NOT EXISTS room_equipment (" +
                         "room_id INTEGER, " +
                         "equipment_id INTEGER, " +
                         "PRIMARY KEY (room_id, equipment_id), " +
                         "FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE, " +
                         "FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id) ON DELETE CASCADE)");

            // Create users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "username TEXT PRIMARY KEY, " +
                         "password TEXT NOT NULL, " +
                         "role TEXT NOT NULL)");

            // Create reservations table
            stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                         "reservation_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "username TEXT NOT NULL, " +
                         "room_id INTEGER NOT NULL, " +
                         "start_time TEXT NOT NULL, " +
                         "end_time TEXT NOT NULL, " +
                         "total_cost REAL NOT NULL, " +
                         "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE, " +
                         "FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE)");

            // Create reservation_equipment table
            stmt.execute("CREATE TABLE IF NOT EXISTS reservation_equipment (" +
                         "reservation_id INTEGER, " +
                         "equipment_id INTEGER, " +
                         "PRIMARY KEY (reservation_id, equipment_id), " +
                         "FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE, " +
                         "FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id) ON DELETE CASCADE)");

            // Insert sample data
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role) VALUES " +
                         "('admin', '" + BCrypt.hashpw("admin", BCrypt.gensalt()) + "', 'ADMIN'), " +
                         "('user1', '" + BCrypt.hashpw("pass123", BCrypt.gensalt()) + "', 'USER')");

            stmt.execute("INSERT OR IGNORE INTO rooms (name, capacity, location, price_per_hour) VALUES " +
                         "('Board Room B', 12, 'Building 1, Floor 2', 50.0), " +
                         "('Conference Room A', 8, 'Building 2, Floor 1', 40.0), " +
                         "('Training Room C', 20, 'Building 3, Floor 3', 60.0)");

            stmt.execute("INSERT OR IGNORE INTO equipment (name, cost) VALUES " +
                         "('Projector', 20.0), " +
                         "('Conference Phone', 15.0), " +
                         "('Whiteboard', 10.0), " +
                         "('Laptop', 30.0)");

            stmt.execute("INSERT OR IGNORE INTO room_equipment (room_id, equipment_id) VALUES " +
                         "((SELECT room_id FROM rooms WHERE name = 'Board Room B'), (SELECT equipment_id FROM equipment WHERE name = 'Projector')), " +
                         "((SELECT room_id FROM rooms WHERE name = 'Board Room B'), (SELECT equipment_id FROM equipment WHERE name = 'Conference Phone')), " +
                         "((SELECT room_id FROM rooms WHERE name = 'Conference Room A'), (SELECT equipment_id FROM equipment WHERE name = 'Whiteboard'))");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }
}
