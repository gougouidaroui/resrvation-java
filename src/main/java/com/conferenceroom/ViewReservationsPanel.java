package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ViewReservationsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private final User user;

    public ViewReservationsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] columns = {"ID", "Room", "Username", "Start Time", "End Time", "Equipment", "Total Cost"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        loadReservations();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton(user.isAdmin() ? "Delete Selected" : "Cancel Selected");
        cancelButton.addActionListener(e -> deleteReservation());
        buttonPanel.add(cancelButton);

        JButton downloadButton = new JButton("Download PDF");
        downloadButton.addActionListener(e -> downloadReservationPDF());
        buttonPanel.add(downloadButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadReservations() {
        tableModel.setRowCount(0);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 user.isAdmin()
                     ? "SELECT r.reservation_id, rm.name, r.username, r.start_time, r.end_time, r.has_equipment, r.total_cost " +
                       "FROM reservations r JOIN rooms rm ON r.room_id = rm.room_id"
                     : "SELECT r.reservation_id, rm.name, r.username, r.start_time, r.end_time, r.has_equipment, r.total_cost " +
                       "FROM reservations r JOIN rooms rm ON r.room_id = rm.room_id WHERE r.username = ?")) {
            if (!user.isAdmin()) {
                stmt.setString(1, user.getUsername());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("reservation_id"),
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("start_time"),
                    rs.getString("end_time"),
                    rs.getInt("has_equipment") == 1 ? "Yes" : "No",
                    String.format("%.2f", rs.getDouble("total_cost"))
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading reservations: " + e.getMessage());
        }
    }

    private void deleteReservation() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a reservation to delete.");
            return;
        }

        int reservationId = (int) tableModel.getValueAt(selectedRow, 0);
        String reservationUsername = (String) tableModel.getValueAt(selectedRow, 2);

        if (!user.isAdmin() && !user.getUsername().equals(reservationUsername)) {
            JOptionPane.showMessageDialog(this, "You can only cancel your own reservations.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this reservation?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM reservations WHERE reservation_id = ?")) {
            stmt.setInt(1, reservationId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Reservation deleted successfully!");
                loadReservations();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete reservation.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting reservation: " + e.getMessage());
        }
    }

    private void downloadReservationPDF() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a reservation to download.");
            return;
        }

        int reservationId = (int) tableModel.getValueAt(selectedRow, 0);
        String roomName = (String) tableModel.getValueAt(selectedRow, 1);
        String username = (String) tableModel.getValueAt(selectedRow, 2);
        String startTime = (String) tableModel.getValueAt(selectedRow, 3);
        String endTime = (String) tableModel.getValueAt(selectedRow, 4);
        String equipment = (String) tableModel.getValueAt(selectedRow, 5);
        double totalCost = Double.parseDouble(((String) tableModel.getValueAt(selectedRow, 6)).replace(",", "."));

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("reservation_" + reservationId + ".pdf"));
        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        try {
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Conference Room Reservation"));
            document.add(new Paragraph("Reservation ID: " + reservationId));
            document.add(new Paragraph("Room: " + roomName));
            document.add(new Paragraph("User: " + username));
            document.add(new Paragraph("Start Time: " + startTime));
            document.add(new Paragraph("End Time: " + endTime));
            document.add(new Paragraph("Equipment Included: " + equipment));
            document.add(new Paragraph("Total Cost: $" + String.format("%.2f", totalCost)));
            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

            document.close();
            JOptionPane.showMessageDialog(this, "PDF downloaded successfully to " + file.getAbsolutePath());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + e.getMessage());
        }
    }
}
