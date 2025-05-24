package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;

public class ViewReservationsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private final App app;
    private final User user;

    public ViewReservationsPanel(App app, User user) {
        this.app = app;
        this.user = user;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 245));

        String[] columns = {"Reservation ID", "Room Name", "Start Time", "End Time", "Total Cost"};
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
        loadReservations();

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.setBackground(new Color(220, 53, 69));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.addActionListener(e -> deleteReservation());
        buttonPanel.add(deleteButton);

        JButton pdfButton = new JButton("Download PDF");
        pdfButton.setBackground(new Color(0, 120, 215));
        pdfButton.setForeground(Color.WHITE);
        pdfButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pdfButton.addActionListener(e -> downloadPDF());
        buttonPanel.add(pdfButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadReservations() {
        tableModel.setRowCount(0);
        String query = user.isAdmin() ?
            "SELECT r.reservation_id, ro.name, r.start_time, r.end_time, r.total_cost " +
            "FROM reservations r JOIN rooms ro ON r.room_id = ro.room_id" :
            "SELECT r.reservation_id, ro.name, r.start_time, r.end_time, r.total_cost " +
            "FROM reservations r JOIN rooms ro ON r.room_id = ro.room_id WHERE r.username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            if (!user.isAdmin()) {
                stmt.setString(1, user.getUsername());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("reservation_id"),
                    rs.getString("name"),
                    rs.getString("start_time"),
                    rs.getString("end_time"),
                    rs.getDouble("total_cost")
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

    private void downloadPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save PDF As");
        fileChooser.setSelectedFile(new File("Reservations.pdf"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        try {
            PdfWriter writer = new PdfWriter(new FileOutputStream(fileToSave));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Reservations Report")
                .setFontSize(18)
                .setBold()
                .setMarginBottom(20));

            float[] columnWidths = {100, 150, 200, 200, 100};
            Table table = new Table(columnWidths);
            table.addHeaderCell(new Cell().add(new Paragraph("Reservation ID").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Room Name").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Start Time").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("End Time").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Total Cost").setBold()));

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(tableModel.getValueAt(i, 0)))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(tableModel.getValueAt(i, 1)))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(tableModel.getValueAt(i, 2)))));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(tableModel.getValueAt(i, 3)))));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", tableModel.getValueAt(i, 4)))));
            }

            document.add(table);
            document.close();
            JOptionPane.showMessageDialog(this, "PDF downloaded successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + e.getMessage());
        }
    }
}
