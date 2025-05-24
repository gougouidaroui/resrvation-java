package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class ReservationPanel extends JPanel {
    private JDatePickerImpl startDatePicker;
    private JSpinner startTimeSpinner;
    private JDatePickerImpl endDatePicker;
    private JSpinner endTimeSpinner;
    private JList<String> equipmentList;
    private DefaultListModel<String> equipmentModel;
    private JLabel defaultEquipmentLabel;
    private final App app;
    private final int roomId;
    private final String roomName;
    private final double pricePerHour;
    private final User user;
    private final Integer reservationId; // Null for new, set for edit

    public ReservationPanel(App app, int roomId, String roomName, double pricePerHour, User user, Integer reservationId) {
        this.app = app;
        this.roomId = roomId;
        this.roomName = roomName;
        this.pricePerHour = pricePerHour;
        this.user = user;
        this.reservationId = reservationId;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Room name
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Room:"), gbc);
        gbc.gridx = 1;
        JLabel roomLabel = new JLabel(roomName);
        roomLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(roomLabel, gbc);

        // Default equipment
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Default Equipment:"), gbc);
        gbc.gridx = 1;
        defaultEquipmentLabel = new JLabel(getDefaultEquipment());
        defaultEquipmentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        add(defaultEquipmentLabel, gbc);

        // Start date picker
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Start Date:"), gbc);
        UtilDateModel startModel = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl startDatePanel = new JDatePanelImpl(startModel, p);
        startDatePicker = new JDatePickerImpl(startDatePanel, new DateLabelFormatter());
        gbc.gridx = 1;
        add(startDatePicker, gbc);

        // Start time spinner
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Start Time:"), gbc);
        SpinnerDateModel startTimeModel = new SpinnerDateModel();
        startTimeSpinner = new JSpinner(startTimeModel);
        JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm");
        startTimeSpinner.setEditor(startTimeEditor);
        gbc.gridx = 1;
        add(startTimeSpinner, gbc);

        // End date picker
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("End Date:"), gbc);
        UtilDateModel endModel = new UtilDateModel();
        JDatePanelImpl endDatePanel = new JDatePanelImpl(endModel, p);
        endDatePicker = new JDatePickerImpl(endDatePanel, new DateLabelFormatter());
        gbc.gridx = 1;
        add(endDatePicker, gbc);

        // End time spinner
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel("End Time:"), gbc);
        SpinnerDateModel endTimeModel = new SpinnerDateModel();
        endTimeSpinner = new JSpinner(endTimeModel);
        JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
        endTimeSpinner.setEditor(endTimeEditor);
        gbc.gridx = 1;
        add(endTimeSpinner, gbc);

        // Additional equipment
        gbc.gridx = 0;
        gbc.gridy = 6;
        add(new JLabel("Additional Equipment:"), gbc);
        gbc.gridx = 1;
        equipmentModel = new DefaultListModel<>();
        equipmentList = new JList<>(equipmentModel);
        equipmentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        loadAvailableEquipment();
        JScrollPane equipmentScroll = new JScrollPane(equipmentList);
        equipmentScroll.setPreferredSize(new Dimension(200, 60));
        add(equipmentScroll, gbc);

        // Reserve/Update button
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.EAST;
        JButton actionButton = new JButton(reservationId == null ? "Reserve" : "Update");
        actionButton.setBackground(new Color(0, 120, 215));
        actionButton.setForeground(Color.WHITE);
        actionButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        actionButton.addActionListener(e -> saveReservation());
        add(actionButton, gbc);

        // Load existing reservation data if editing
        if (reservationId != null) {
            loadReservationData();
        }
    }

    private String getDefaultEquipment() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT e.name FROM equipment e JOIN room_equipment re ON e.equipment_id = re.equipment_id WHERE re.room_id = ?")) {
            stmt.setInt(1, roomId);
            ResultSet rs = stmt.executeQuery();
            List<String> equipment = new ArrayList<>();
            while (rs.next()) {
                equipment.add(rs.getString("name"));
            }
            return equipment.isEmpty() ? "None" : String.join(", ", equipment);
        } catch (SQLException e) {
            return "Error loading equipment";
        }
    }

    private void loadAvailableEquipment() {
        equipmentModel.clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM equipment")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                equipmentModel.addElement(rs.getString("name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading equipment: " + e.getMessage());
        }
    }

    private void loadReservationData() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT start_time, end_time FROM reservations WHERE reservation_id = ?")) {
            stmt.setInt(1, reservationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime start = LocalDateTime.parse(rs.getString("start_time"), formatter);
                LocalDateTime end = LocalDateTime.parse(rs.getString("end_time"), formatter);

                startDatePicker.getModel().setDate(start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth());
                startDatePicker.getModel().setSelected(true);
                startTimeSpinner.setValue(Date.from(start.toInstant(ZoneOffset.UTC)));
                endDatePicker.getModel().setDate(end.getYear(), end.getMonthValue() - 1, end.getDayOfMonth());
                endDatePicker.getModel().setSelected(true);
                endTimeSpinner.setValue(Date.from(end.toInstant(ZoneOffset.UTC)));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading reservation: " + e.getMessage());
        }

        // Load selected equipment
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT e.name FROM equipment e JOIN reservation_equipment re ON e.equipment_id = re.equipment_id WHERE re.reservation_id = ?")) {
            stmt.setInt(1, reservationId);
            ResultSet rs = stmt.executeQuery();
            List<String> selected = new ArrayList<>();
            while (rs.next()) {
                selected.add(rs.getString("name"));
            }
            equipmentList.setSelectedIndices(getSelectedIndices(selected));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading equipment: " + e.getMessage());
        }
    }

    private int[] getSelectedIndices(List<String> selected) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < equipmentModel.size(); i++) {
            if (selected.contains(equipmentModel.getElementAt(i))) {
                indices.add(i);
            }
        }
        return indices.stream().mapToInt(i -> i).toArray();
    }

    private void saveReservation() {
        Object startDateValue = startDatePicker.getModel().getValue();
        Object endDateValue = endDatePicker.getModel().getValue();
        java.util.Date startTime = (java.util.Date) startTimeSpinner.getValue();
        java.util.Date endTime = (java.util.Date) endTimeSpinner.getValue();

        if (startDateValue == null || endDateValue == null) {
            JOptionPane.showMessageDialog(this, "Please select both start and end dates.");
            return;
        }

        try {
            LocalDateTime start = convertToLocalDateTime(startDateValue, startTime);
            LocalDateTime end = convertToLocalDateTime(endDateValue, endTime);
            List<String> selectedEquipment = equipmentList.getSelectedValuesList();

            if (!end.isAfter(start)) {
                JOptionPane.showMessageDialog(this, "End time must be after start time.");
                return;
            }

            // Check for overlapping reservations
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM reservations r WHERE r.room_id = ? AND " +
                     "(r.start_time < ? AND r.end_time > ?) AND r.reservation_id != ?")) {
                stmt.setInt(1, roomId);
                stmt.setString(2, end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                stmt.setString(3, start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                stmt.setInt(4, reservationId != null ? reservationId : 0);
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "This time slot is already reserved for the selected room.");
                    return;
                }
            }

            // Calculate cost
            long minutes = ChronoUnit.MINUTES.between(start, end);
            double hours = minutes / 60.0;
            double roomCost = hours * pricePerHour;
            double equipmentCost = calculateEquipmentCost(selectedEquipment);
            double totalCost = roomCost + equipmentCost;

            // Format times
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String startTimeStr = start.format(formatter);
            String endTimeStr = end.format(formatter);

            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    Integer newReservationId;
                    if (reservationId == null) {
                        // Insert new reservation
                        try (PreparedStatement stmt = conn.prepareStatement(
                             "INSERT INTO reservations (username, room_id, start_time, end_time, total_cost) VALUES (?, ?, ?, ?, ?)",
                             PreparedStatement.RETURN_GENERATED_KEYS)) {
                            stmt.setString(1, user.getUsername());
                            stmt.setInt(2, roomId);
                            stmt.setString(3, startTimeStr);
                            stmt.setString(4, endTimeStr);
                            stmt.setDouble(5, totalCost);
                            stmt.executeUpdate();
                            ResultSet rs = stmt.getGeneratedKeys();
                            newReservationId = rs.next() ? rs.getInt(1) : null;
                        }
                    } else {
                        // Update existing reservation
                        try (PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE reservations SET room_id = ?, start_time = ?, end_time = ?, total_cost = ? WHERE reservation_id = ?")) {
                            stmt.setInt(1, roomId);
                            stmt.setString(2, startTimeStr);
                            stmt.setString(3, endTimeStr);
                            stmt.setDouble(4, totalCost);
                            stmt.setInt(5, reservationId);
                            stmt.executeUpdate();
                        }
                        newReservationId = reservationId;
                        // Clear existing equipment
                        try (PreparedStatement stmt = conn.prepareStatement(
                             "DELETE FROM reservation_equipment WHERE reservation_id = ?")) {
                            stmt.setInt(1, reservationId);
                            stmt.executeUpdate();
                        }
                    }

                    // Insert selected equipment
                    for (String equip : selectedEquipment) {
                        try (PreparedStatement stmt = conn.prepareStatement(
                             "INSERT INTO reservation_equipment (reservation_id, equipment_id) VALUES (?, (SELECT equipment_id FROM equipment WHERE name = ?))")) {
                            stmt.setInt(1, newReservationId);
                            stmt.setString(2, equip);
                            stmt.executeUpdate();
                        }
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(this, (reservationId == null ? "Reservation" : "Update") + " successful! Total Cost: $" + String.format("%.2f", totalCost));
                    app.showMainPanel(user);
                } catch (SQLException e) {
                    conn.rollback();
                    JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error processing date/time: " + e.getMessage());
        }
    }

    private double calculateEquipmentCost(List<String> selectedEquipment) {
        double cost = 0.0;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT cost FROM equipment WHERE name = ?")) {
            for (String equip : selectedEquipment) {
                stmt.setString(1, equip);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    cost += rs.getDouble("cost");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error calculating equipment cost: " + e.getMessage());
        }
        return cost;
    }

    private LocalDateTime convertToLocalDateTime(Object dateObj, java.util.Date time) {
        LocalDate localDate;
        if (dateObj instanceof GregorianCalendar calendar) {
            localDate = calendar.toZonedDateTime().toLocalDate();
        } else if (dateObj instanceof java.util.Date date) {
            localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            throw new IllegalArgumentException("Unsupported date type: " + dateObj.getClass());
        }

        Instant timeInstant = time.toInstant();
        LocalTime localTime = timeInstant.atZone(ZoneId.systemDefault()).toLocalTime();

        return LocalDateTime.of(localDate, localTime);
    }

    private static class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public Object stringToValue(String text) throws java.text.ParseException {
            return LocalDate.parse(text, formatter);
        }

        @Override
        public String valueToString(Object value) throws java.text.ParseException {
            if (value == null) {
                return "";
            }
            LocalDate date;
            if (value instanceof GregorianCalendar calendar) {
                date = calendar.toZonedDateTime().toLocalDate();
            } else if (value instanceof java.util.Date dateValue) {
                date = dateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                throw new java.text.ParseException("Unsupported date type: " + value.getClass(), 0);
            }
            return date.format(formatter);
        }
    }
}
