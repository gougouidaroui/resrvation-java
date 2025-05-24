package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomManagementPanel extends JPanel {
    private JTextField nameField;
    private JTextField capacityField;
    private JTextField locationField;
    private JTextField priceField;
    private JList<String> defaultEquipmentList;
    private DefaultListModel<String> defaultEquipmentModel;
    private JTable table;
    private DefaultTableModel tableModel;

    public RoomManagementPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 245));

        // Add room form
        JPanel addRoomPanel = new JPanel(new GridBagLayout());
        addRoomPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel capacityLabel = new JLabel("Capacity:");
        capacityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(capacityLabel, gbc);
        gbc.gridx = 1;
        capacityField = new JTextField(10);
        capacityField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(capacityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel locationLabel = new JLabel("Location:");
        locationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(locationLabel, gbc);
        gbc.gridx = 1;
        locationField = new JTextField(15);
        locationField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(locationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel priceLabel = new JLabel("Price/Hour:");
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(priceLabel, gbc);
        gbc.gridx = 1;
        priceField = new JTextField(10);
        priceField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(priceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel equipmentLabel = new JLabel("Default Equipment:");
        equipmentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addRoomPanel.add(equipmentLabel, gbc);
        gbc.gridx = 1;
        defaultEquipmentModel = new DefaultListModel<>();
        defaultEquipmentList = new JList<>(defaultEquipmentModel);
        defaultEquipmentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        loadAvailableEquipment();
        JScrollPane equipmentScroll = new JScrollPane(defaultEquipmentList);
        equipmentScroll.setPreferredSize(new Dimension(200, 60));
        addRoomPanel.add(equipmentScroll, gbc);

        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        JButton addButton = new JButton("Add Room");
        addButton.setBackground(new Color(0, 120, 215));
        addButton.setForeground(Color.WHITE);
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addButton.addActionListener(e -> addRoom());
        addRoomPanel.add(addButton, gbc);

        add(addRoomPanel, BorderLayout.NORTH);

        // Room list table
        String[] columns = {"Name", "Capacity", "Location", "Price/Hour"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        loadRooms();

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Delete button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setBackground(new Color(220, 53, 69));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.addActionListener(e -> deleteRoom());
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadAvailableEquipment() {
        defaultEquipmentModel.clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM equipment")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                defaultEquipmentModel.addElement(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading equipment: " + e.getMessage());
        }
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
        List<String> selectedEquipment = defaultEquipmentList.getSelectedValuesList();

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

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert room
                int roomId;
                try (PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO rooms (name, capacity, location, price_per_hour) VALUES (?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, name);
                    stmt.setInt(2, capacity);
                    stmt.setString(3, location);
                    stmt.setDouble(4, price);
                    int rows = stmt.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("Failed to add room.");
                    }
                    ResultSet rs = stmt.getGeneratedKeys();
                    roomId = rs.next() ? rs.getInt(1) : -1;
                }

                // Insert default equipment
                for (String equip : selectedEquipment) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO room_equipment (room_id, equipment_id) VALUES (?, (SELECT equipment_id FROM equipment WHERE name = ?))")) {
                        stmt.setInt(1, roomId);
                        stmt.setString(2, equip);
                        stmt.executeUpdate();
                    }
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Room added successfully!");
                nameField.setText("");
                capacityField.setText("");
                locationField.setText("");
                priceField.setText("");
                defaultEquipmentList.clearSelection();
                loadRooms();
            } catch (SQLException e) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Error adding room: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
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
            conn.setAutoCommit(false);
            try {
                // Delete associated reservations
                try (PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM reservations WHERE room_id = (SELECT r.room_id FROM rooms r WHERE r.name = ?)")) {
                    stmt.setString(1, roomName);
                    stmt.executeUpdate();
                }

                // Delete room equipment
                try (PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM room_equipment WHERE room_id = (SELECT r.room_id FROM rooms r WHERE r.name = ?)")) {
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

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Error deleting room: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }
}
