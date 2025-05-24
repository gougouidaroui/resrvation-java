package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class RoomsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private final App app;

    public RoomsPanel(App app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Table setup
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

        JButton reserveButton = new JButton("Reserve Selected Room");
        reserveButton.addActionListener(e -> reserveRoom());
        add(reserveButton, BorderLayout.SOUTH);
    }

    private void loadRooms() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT room_id, name, capacity, location, price_per_hour FROM rooms");
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

    private void reserveRoom() {
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
