package com.conferenceroom;

import com.conferenceroom.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

public class LoginPanel extends JPanel {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private final App app;

    public LoginPanel(App app) {
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

        // Login button
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> attemptLogin());
        add(new JLabel(""));
        add(loginButton);

        // Register button
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> app.showRegisterPanel());
        add(new JLabel(""));
        add(registerButton);
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            User user = AuthService.login(username, password);
            if (user != null) {
                app.showMainPanel(user);
                JOptionPane.showMessageDialog(this, "Login successful! Welcome, " + username);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error during login: " + e.getMessage());
        }
    }
}
