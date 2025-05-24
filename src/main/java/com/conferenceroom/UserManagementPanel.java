package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class UserManagementPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;

    public UserManagementPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"Username", "Role"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        table = new JTable(tableModel);
        loadUsers();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteUser());
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadUsers() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT username, role FROM users");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("username"),
                    rs.getString("role")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage());
        }
    }

    private void deleteUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.");
            return;
        }

        String username = (String) tableModel.getValueAt(selectedRow, 0);
        if (username.equals("admin")) {
            JOptionPane.showMessageDialog(this, "Cannot delete the default admin user.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete user " + username + "? This will also delete their reservations.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // Delete user's reservations
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM reservations WHERE username = ?")) {
                stmt.setString(1, username);
                stmt.executeUpdate();
            }

            // Delete user
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE username = ?")) {
                stmt.setString(1, username);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "User deleted successfully!");
                    loadUsers();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete user.");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting user: " + e.getMessage());
        }
    }
}
