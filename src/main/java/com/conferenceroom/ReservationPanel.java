package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

public class ReservationPanel extends JPanel {
    private JDatePickerImpl startDatePicker;
    private JSpinner startTimeSpinner;
    private JDatePickerImpl endDatePicker;
    private JSpinner endTimeSpinner;
    private JCheckBox equipmentCheckBox;
    private final App app;
    private final int roomId;
    private final String roomName;
    private final double pricePerHour;
    private final User user;

    public ReservationPanel(App app, int roomId, String roomName, double pricePerHour, User user) {
        this.app = app;
        this.roomId = roomId;
        this.roomName = roomName;
        this.pricePerHour = pricePerHour;
        this.user = user;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Room name (display only)
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Room:"), gbc);
        gbc.gridx = 1;
        add(new JLabel(roomName), gbc);

        // Start date picker
        gbc.gridx = 0;
        gbc.gridy = 1;
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
        gbc.gridy = 2;
        add(new JLabel("Start Time:"), gbc);
        SpinnerDateModel startTimeModel = new SpinnerDateModel();
        startTimeSpinner = new JSpinner(startTimeModel);
        JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(startTimeSpinner, "HH:mm");
        startTimeSpinner.setEditor(startTimeEditor);
        gbc.gridx = 1;
        add(startTimeSpinner, gbc);

        // End date picker
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("End Date:"), gbc);
        UtilDateModel endModel = new UtilDateModel();
        JDatePanelImpl endDatePanel = new JDatePanelImpl(endModel, p);
        endDatePicker = new JDatePickerImpl(endDatePanel, new DateLabelFormatter());
        gbc.gridx = 1;
        add(endDatePicker, gbc);

        // End time spinner
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("End Time:"), gbc);
        SpinnerDateModel endTimeModel = new SpinnerDateModel();
        endTimeSpinner = new JSpinner(endTimeModel);
        JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(endTimeSpinner, "HH:mm");
        endTimeSpinner.setEditor(endTimeEditor);
        gbc.gridx = 1;
        add(endTimeSpinner, gbc);

        // Equipment
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel("Include Equipment:"), gbc);
        equipmentCheckBox = new JCheckBox();
        gbc.gridx = 1;
        add(equipmentCheckBox, gbc);

        // Reserve button
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.EAST;
        JButton reserveButton = new JButton("Reserve");
        reserveButton.addActionListener(e -> makeReservation());
        add(reserveButton, gbc);
    }

    private void makeReservation() {
        // Get date and time values
        Object startDateValue = startDatePicker.getModel().getValue();
        Object endDateValue = endDatePicker.getModel().getValue();
        Date startTime = (Date) startTimeSpinner.getValue();
        Date endTime = (Date) endTimeSpinner.getValue();

        if (startDateValue == null || endDateValue == null) {
            JOptionPane.showMessageDialog(this, "Please select both start and end dates.");
            return;
        }

        try {
            // Convert to LocalDateTime
            LocalDateTime start = convertToLocalDateTime(startDateValue, startTime);
            LocalDateTime end = convertToLocalDateTime(endDateValue, endTime);
            boolean hasEquipment = equipmentCheckBox.isSelected();

            // Validate times
            if (!end.isAfter(start)) {
                JOptionPane.showMessageDialog(this, "End time must be after start time.");
                return;
            }

            // Check for overlapping reservations
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM reservations WHERE room_id = ? AND " +
                     "(start_time < ? AND end_time > ?)")) {
                stmt.setInt(1, roomId);
                stmt.setString(2, end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                stmt.setString(3, start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "This time slot is already reserved for the selected room.");
                    return;
                }
            }

            // Format for database
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String startTimeStr = start.format(formatter);
            String endTimeStr = end.format(formatter);

            // Calculate duration and cost
            long minutes = ChronoUnit.MINUTES.between(start, end);
            double hours = minutes / 60.0;
            double totalCost = hours * pricePerHour;
            if (hasEquipment) {
                totalCost += 20.0; // Example: $20 flat fee for equipment
            }

            // Save reservation
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO reservations (username, room_id, start_time, end_time, has_equipment, total_cost) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, user.getUsername());
                stmt.setInt(2, roomId);
                stmt.setString(3, startTimeStr);
                stmt.setString(4, endTimeStr);
                stmt.setInt(5, hasEquipment ? 1 : 0);
                stmt.setDouble(6, totalCost);

                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, "Reservation successful! Total Cost: $" + String.format("%.2f", totalCost));
                    app.showMainPanel(user);
                } else {
                    JOptionPane.showMessageDialog(this, "Reservation failed.");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error processing date/time: " + e.getMessage());
        }
    }

    private LocalDateTime convertToLocalDateTime(Object dateObj, Date time) {
        LocalDate localDate;
        if (dateObj instanceof GregorianCalendar calendar) {
            localDate = calendar.toZonedDateTime().toLocalDate();
        } else if (dateObj instanceof Date date) {
            localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            throw new IllegalArgumentException("Unsupported date type: " + dateObj.getClass());
        }

        Instant timeInstant = time.toInstant();
        LocalTime localTime = timeInstant.atZone(ZoneId.systemDefault()).toLocalTime();

        return LocalDateTime.of(localDate, localTime);
    }

    // Formatter for JDatePicker
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
            } else if (value instanceof Date dateValue) {
                date = dateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                throw new java.text.ParseException("Unsupported date type: " + value.getClass(), 0);
            }
            return date.format(formatter);
        }
    }
}
