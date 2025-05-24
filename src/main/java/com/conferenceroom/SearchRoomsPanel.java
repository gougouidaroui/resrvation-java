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
        setBackground(new Color(245, 245, 245));

        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Room Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel minPriceLabel = new JLabel("Min Price/Hour:");
        minPriceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(minPriceLabel, gbc);
        gbc.gridx = 1;
        minPriceField = new JTextField(10);
        minPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(minPriceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel maxPriceLabel = new JLabel("Max Price/Hour:");
        maxPriceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(maxPriceLabel, gbc);
        gbc.gridx = 1;
        maxPriceField = new JTextField(10);
        maxPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(maxPriceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel minCapacityLabel = new JLabel("Min Capacity:");
        minCapacityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(minCapacityLabel, gbc);
        gbc.gridx = 1;
        minCapacityField = new JTextField(10);
        minCapacityField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchPanel.add(minCapacityField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.EAST;
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(0, 120, 215));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchButton.addActionListener(e -> searchRooms());
        searchPanel.add(searchButton, gbc);

        add(searchPanel, BorderLayout.NORTH);

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
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton reserveButton = new JButton("Reserve Selected Room");
        reserveButton.setBackground(new Color(0, 120, 215));
        reserveButton.setForeground(Color.WHITE);
        reserveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reserveButton.addActionListener(e -> reserveRoom(app));
        buttonPanel.add(reserveButton);
        add(buttonPanel, BorderLayout.SOUTH);
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
                app.showReservationPanel(roomId, roomName, pricePerHour, app.getCurrentUser());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error selecting room: " + e.getMessage());
        }
    }
}
