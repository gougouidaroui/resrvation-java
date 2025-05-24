package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;

public class App extends JFrame {
    private JPanel mainPanel;
    private User currentUser;
    private JTabbedPane tabbedPane;

    public App() {
        // Set FlatLaf look and feel
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("TabbedPane.tabInsets", new Insets(10, 20, 10, 20));
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 12));
            UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 12));
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf: " + e.getMessage());
        }

        setTitle("Conference Room Reservation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 245, 245));

        // Initialize database
        DatabaseManager.initializeDatabase();

        // Show login panel
        mainPanel = new LoginPanel(this);
        add(mainPanel);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void showMainPanel(User user) {
        this.currentUser = user;
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(245, 245, 245));

        // Add logout button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 245, 245));
        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(220, 53, 69));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutButton.addActionListener(e -> showLoginPanel());
        buttonPanel.add(logoutButton);
        mainPanel.add(buttonPanel, BorderLayout.NORTH);

        // Add tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.addTab("Rooms", new RoomsPanel(this));
        tabbedPane.addTab("Search Rooms", new SearchRoomsPanel(this));
        tabbedPane.addTab("Reservations", new ViewReservationsPanel(this, user));
        if (user.isAdmin()) {
            tabbedPane.addTab("User Management", new UserManagementPanel());
            tabbedPane.addTab("Room Management", new RoomManagementPanel());
            tabbedPane.addTab("Equipment Management", new EquipmentManagementPanel());
        }
        tabbedPane.setSelectedIndex(0);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void showLoginPanel() {
        this.currentUser = null;
        mainPanel.removeAll();
        mainPanel = new LoginPanel(this);
        add(mainPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void showRegisterPanel() {
        mainPanel.removeAll();
        mainPanel = new RegisterPanel(this);
        add(mainPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void showReservationPanel(int roomId, String roomName, double pricePerHour, User user, Integer reservationId) {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(new ReservationPanel(this, roomId, roomName, pricePerHour, user, reservationId), BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new App().setVisible(true);
        });
    }
}
