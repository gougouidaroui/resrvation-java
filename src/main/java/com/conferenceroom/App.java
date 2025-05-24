package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;

public class App extends JFrame {
    private JPanel mainPanel;
    private User currentUser;
    private JTabbedPane tabbedPane;

    public App() {
        setTitle("Conference Room Reservation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        // Initialize database
        DatabaseManager.initializeDatabase();

        // Show login panel
        mainPanel = new LoginPanel(this);
        add(mainPanel);
    }

    public void showMainPanel(User user) {
        this.currentUser = user;
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());

        // Add logout button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> showLoginPanel());
        buttonPanel.add(logoutButton);
        mainPanel.add(buttonPanel, BorderLayout.NORTH);

        // Add tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Rooms", new RoomsPanel(this));
        tabbedPane.addTab("Search Rooms", new SearchRoomsPanel(this));
        tabbedPane.addTab("Reservations", new ViewReservationsPanel(user));
        if (user.isAdmin()) {
            tabbedPane.addTab("User Management", new UserManagementPanel());
        }
        tabbedPane.setSelectedIndex(0); // Select Rooms tab

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

    public void showReservationPanel(int roomId, String roomName, double pricePerHour) {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(new ReservationPanel(this, roomId, roomName, pricePerHour, currentUser), BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new App().setVisible(true);
        });
    }
}
