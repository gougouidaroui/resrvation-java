package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class EquipmentManagementPanel extends JPanel {
    private JTextField nameField;
    private JTextField costField;
    private JTable table;
    private DefaultTableModel tableModel;

    public EquipmentManagementPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 245));

        // Add equipment form
        JPanel addEquipmentPanel = new JPanel(new GridBagLayout());
        addEquipmentPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Equipment Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addEquipmentPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        nameField = new JTextField(15);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addEquipmentPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel costLabel = new JLabel("Cost:");
        costLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addEquipmentPanel.add(costLabel, gbc);
        gbc.gridx = 1;
        costField = new JTextField(10);
        costField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addEquipmentPanel.add(costField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JButton addButton = new JButton("Add Equipment");
        addButton.setBackground(new Color(0, 120, 215));
        addButton.setForeground(Color.WHITE);
        addButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addButton.addActionListener(e -> addEquipment());
        addEquipmentPanel.add(addButton, gbc);

        add(addEquipmentPanel, BorderLayout.NORTH);

        // Equipment list table
        String[] columns = {"Name", "Cost"};
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
        loadEquipment();

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Delete button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setBackground(new Color(220, 53, 69));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.addActionListener(e -> deleteEquipment());
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadEquipment() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name, cost FROM equipment");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getDouble("cost")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading equipment: " + e.getMessage());
        }
    }

    private void addEquipment() {
        String name = nameField.getText().trim();
        String costStr = costField.getText().trim();

        if (name.isEmpty() || costStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and cost are required.");
            return;
        }

        double cost;
        try {
            cost = Double.parseDouble(costStr);
            if (cost < 0) {
                JOptionPane.showMessageDialog(this, "Cost must be a non-negative number.");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cost must be a valid number.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO equipment (name, cost) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setDouble(2, cost);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Equipment added successfully!");
                nameField.setText("");
                costField.setText("");
                loadEquipment();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add equipment.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding equipment: " + e.getMessage());
        }
    }

    private void deleteEquipment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an equipment item to delete.");
            return;
        }

        String equipmentName = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete equipment " + equipmentName + "? This will remove it from all rooms and reservations.", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete from reservation_equipment
                try (PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM reservation_equipment WHERE equipment_id = (SELECT equipment_id FROM equipment WHERE name = ?)")) {
                    stmt.setString(1, equipmentName);
                    stmt.executeUpdate();
                }

                // Delete from room_equipment
                try (PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM room_equipment WHERE equipment_id = (SELECT equipment_id FROM equipment WHERE name = ?)")) {
                    stmt.setString(1, equipmentName);
                    stmt.executeUpdate();
                }

                // Delete equipment
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM equipment WHERE name = ?")) {
                    stmt.setString(1, equipmentName);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Equipment deleted successfully!");
                        loadEquipment();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to delete equipment.");
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Error deleting equipment: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }
}
