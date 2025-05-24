package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class SearchRoomsPanel extends JPanel {
    private JTextField nameField;
    private JTextField minPriceField;
    private JTextField maxPriceField;
    private JTextField minCapacityField;
    private JTable table;
    private DefaultTableModel tableModel;

    public SearchRoomsPanel(App app) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Search form
        JPanel searchPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        searchPanel.add(new JLabel("Room Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        searchPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        searchPanel.add(new JLabel("Min Price/Hour:"), gbc);
        gbc.gridx = 1;
        minPriceField = new JTextField(10);
        searchPanel.add(minPriceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        searchPanel.add(new JLabel("Max Price/Hour:"), gbc);
        gbc.gridx = 1;
        maxPriceField = new JTextField(10);
        searchPanel.add(maxPriceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        searchPanel.add(new JLabel("Min Capacity:"), gbc);
        gbc.gridx = 1;
        minCapacityField = new JTextField(10);
        searchPanel.add(minCapacityField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchRooms());
        searchPanel.add(searchButton, gbc);

        add(searchPanel, BorderLayout.NORTH);

        // Results table
        String[] columns = {"Name", "Capacity", "Location", "Price/Hour"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton reserveButton = new JButton("Reserve Selected Room");
        reserveButton.addActionListener(e -> reserveRoom(app));
        add(reserveButton, BorderLayout.SOUTH);
    }

    private void searchRooms() {
        tableModel.setRowCount(0);
        String name = nameField.getText().trim();
        String minPrice = minPriceField.getText().trim();
        String maxPrice = maxPriceField.getText().trim();
        String minCapacity = minCapacityField.getText().trim();

        StringBuilder query = new StringBuilder("SELECT room_id, name, capacity, location, price_per_hour FROM rooms WHERE 1=1");
        StringBuilder conditions = new StringBuilder();
        int paramIndex = 1;

        if (!name.isEmpty()) {
            conditions.append(" AND name LIKE ?");
        }
        if (!minPrice.isEmpty()) {
            try {
                Double.parseDouble(minPrice);
                conditions.append(" AND price_per_hour >= ?");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Minimum price must be a valid number.");
                return;
            }
        }
        if (!maxPrice.isEmpty()) {
            try {
                Double.parseDouble(maxPrice);
                conditions.append(" AND price_per_hour <= ?");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Maximum price must be a valid number.");
                return;
            }
        }
        if (!minCapacity.isEmpty()) {
            try {
                Integer.parseInt(minCapacity);
                conditions.append(" AND capacity >= ?");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Minimum capacity must be a valid integer.");
                return;
            }
        }

        query.append(conditions);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            if (!name.isEmpty()) {
                stmt.setString(paramIndex++, "%" + name + "%");
            }
            if (!minPrice.isEmpty()) {
                stmt.setDouble(paramIndex++, Double.parseDouble(minPrice));
            }
            if (!maxPrice.isEmpty()) {
                stmt.setDouble(paramIndex++, Double.parseDouble(maxPrice));
            }
            if (!minCapacity.isEmpty()) {
                stmt.setInt(paramIndex++, Integer.parseInt(minCapacity));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getInt("capacity"),
                    rs.getString("location"),
                    rs.getDouble("price_per_hour")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error searching rooms: " + e.getMessage());
        }
    }

    private void reserveRoom(App app) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to reserve.");
            return;
        }

        String roomName = (String) tableModel.getValueAt(selectedRow, 0);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT room_id, price_per_hour FROM rooms WHERE name = ?")) {
            stmt.setString(1, roomName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int roomId = rs.getInt("room_id");
                double pricePerHour = rs.getDouble("price_per_hour");
                app.showReservationPanel(roomId, roomName, pricePerHour);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error selecting room: " + e.getMessage());
        }
    }
}
