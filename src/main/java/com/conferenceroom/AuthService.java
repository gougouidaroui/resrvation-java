package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class AuthService {
    public static User login(String username, String password) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT password, role FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    return new User(username, rs.getString("role"));
                }
            }
            return null;
        }
    }

    public static boolean registerUser(String username, String password) throws SQLException {
        // Check if username exists
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            if (stmt.executeQuery().next()) {
                return false; // Username already taken
            }
        }

        // Register new user with USER role
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, 'USER')")) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.executeUpdate();
            return true;
        }
    }
}
