package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

public class RegisterPanel extends JPanel {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private final App app;

    public RegisterPanel(App app) {
        this.app = app;
        setLayout(new GridLayout(4, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Username field
        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField);

        // Password field
        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField);

        // Register button
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> attemptRegister());
        add(new JLabel(""));
        add(registerButton);

        // Back to Login button
        JButton backButton = new JButton("Back to Login");
        backButton.addActionListener(e -> app.showLoginPanel());
        add(new JLabel(""));
        add(backButton);
    }

    private void attemptRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        try {
            boolean success = AuthService.registerUser(username, password);
            if (success) {
                JOptionPane.showMessageDialog(this, "Registration successful! Please log in.");
                app.showLoginPanel();
            } else {
                JOptionPane.showMessageDialog(this, "Username already taken.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error during registration: " + e.getMessage());
        }
    }
}
