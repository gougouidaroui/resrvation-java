package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class RoomManagementPanel extends JPanel {
    private JTextField nameField;
    private JTextField capacityField;
    private JTextField locationField;
    private JTextField priceField;
    private JTable table;
    private DefaultTableModel tableModel;

    public RoomManagementPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add room form
        JPanel addRoomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        addRoomPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        addRoomPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        addRoomPanel.add(new JLabel("Capacity:"), gbc);
        gbc.gridx = 1;
        capacityField = new JTextField(10);
        addRoomPanel.add(capacityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        addRoomPanel.add(new JLabel("Location:"), gbc);
        gbc.gridx = 1;
        locationField = new JTextField(15);
        addRoomPanel.add(locationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        addRoomPanel.add(new JLabel("Price/Hour:"), gbc);
        gbc.gridx = 1;
        priceField = new JTextField(10);
        addRoomPanel.add(priceField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        JButton addButton = new JButton("Add Room");
        addButton.addActionListener(e -> addRoom());
        addRoomPanel.add(addButton, gbc);

        add(addRoomPanel, BorderLayout.NORTH);

        // Room list table
        String[] columns = {"Name", "Capacity", "Location", "Price/Hour"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        table = new JTable(tableModel);
        loadRooms();

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Delete button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteRoom());
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadRooms() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name, capacity, location, price_per_hour FROM rooms");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getInt("capacity"),
                    rs.getString("location"),
                    rs.getDouble("price_per_hour")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading rooms: " + e.getMessage());
        }
    }

    private void addRoom() {
        String name = nameField.getText().trim();
        String capacityStr = capacityField.getText().trim();
        String location = locationField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (name.isEmpty() || capacityStr.isEmpty() || location.isEmpty() || priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        int capacity;
        double price;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) {
                JOptionPane.showMessageDialog(this, "Capacity must be a positive integer.");
                return;
            }
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                JOptionPane.showMessageDialog(this, "Price must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Capacity must be an integer and Price must be a number.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO rooms (name, capacity, location, price_per_hour) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, name);
            stmt.setInt(2, capacity);
            stmt.setString(3, location);
            stmt.setDouble(4, price);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Room added successfully!");
                nameField.setText("");
                capacityField.setText("");
                locationField.setText("");
                priceField.setText("");
                loadRooms();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add room.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding room: " + e.getMessage());
        }
    }

    private void deleteRoom() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to delete.");
            return;
        }

        String roomName = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete room " + roomName + "? This will also delete associated reservations.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // Delete associated reservations
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM reservations WHERE room_id IN (SELECT room_id FROM rooms WHERE name = ?)")) {
                stmt.setString(1, roomName);
                stmt.executeUpdate();
            }

            // Delete room
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM rooms WHERE name = ?")) {
                stmt.setString(1, roomName);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Room deleted successfully!");
                    loadRooms();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete room.");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting room: " + e.getMessage());
        }
    }
}
