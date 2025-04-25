package javafullstack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.Timer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    private static Connection connection;
    private static Timer studyTimer;
    private static int remainingSeconds;
    private static JLabel timerLabel;
    private static String currentUser;
    private static JComboBox<String> subjectComboBox;
    private static DefaultComboBoxModel<String> subjectModel;
    private static JTextArea historyArea;
    private static JFrame mainFrame;

    public static void main(String[] args) {
        initializeDatabase();
        SwingUtilities.invokeLater(Main::showLoginPage);
    }

    private static void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/study_planner",
                "root", 
                "Root123" // Change to your MySQL password
            );
        } catch (Exception e) {
            showError("Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void showLoginPage() {
        JFrame frame = new JFrame("Study Planner - Login");
        frame.setSize(350, 200);
        frame.setLayout(new GridLayout(3, 2, 10, 10));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> handleLogin(frame, usernameField, passwordField));

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> showRegistrationPage());

        frame.add(new JLabel("Username:"));
        frame.add(usernameField);
        frame.add(new JLabel("Password:"));
        frame.add(passwordField);
        frame.add(loginButton);
        frame.add(registerButton);
        centerFrame(frame);
        frame.setVisible(true);
    }

    private static void handleLogin(JFrame frame, JTextField usernameField, JPasswordField passwordField) {
        try {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            if (validateUser(username, password)) {
                currentUser = username;
                frame.dispose();
                showMainPlanner();
            } else {
                showError("Invalid credentials");
            }
        } catch (Exception ex) {
            showError("Login error: " + ex.getMessage());
        }
    }

    private static void showRegistrationPage() {
        JFrame frame = new JFrame("Registration");
        frame.setSize(350, 150);
        frame.setLayout(new GridLayout(3, 2, 10, 10));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> handleRegistration(frame, usernameField, passwordField));

        frame.add(new JLabel("Username:"));
        frame.add(usernameField);
        frame.add(new JLabel("Password:"));
        frame.add(passwordField);
        frame.add(new JLabel());
        frame.add(registerButton);
        centerFrame(frame);
        frame.setVisible(true);
    }

    private static void handleRegistration(JFrame frame, JTextField usernameField, JPasswordField passwordField) {
        try {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            if (registerUser(username, password)) {
                showInfo("Registration successful!");
                frame.dispose();
            } else {
                showError("Username already exists");
            }
        } catch (Exception ex) {
            showError("Registration error: " + ex.getMessage());
        }
    }

    private static void showMainPlanner() {
        mainFrame = new JFrame("Study Planner - " + currentUser);
        mainFrame.setSize(700, 500);
        mainFrame.setLayout(new BorderLayout(10, 10));
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        createSubjectPanel();
        createTimerPanel();
        createHistoryPanel();
        createControlPanel();

        loadSubjects();
        loadSessionHistory();
        centerFrame(mainFrame);
        mainFrame.setVisible(true);
    }

    private static void createSubjectPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        subjectModel = new DefaultComboBoxModel<>();
        subjectComboBox = new JComboBox<>(subjectModel);
        
        JButton addSubjectButton = new JButton("Add Subject");
        addSubjectButton.addActionListener(e -> showAddSubjectDialog());

        panel.add(new JLabel("Subject:"), BorderLayout.WEST);
        panel.add(subjectComboBox, BorderLayout.CENTER);
        panel.add(addSubjectButton, BorderLayout.EAST);
        mainFrame.add(panel, BorderLayout.NORTH);
    }

    private static void createTimerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        timerLabel = new JLabel("25:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 48));
        timerLabel.setForeground(new Color(0, 100, 0));
        panel.add(timerLabel, BorderLayout.CENTER);
        mainFrame.add(panel, BorderLayout.CENTER);
    }

    private static void createHistoryPanel() {
        historyArea = new JTextArea(10, 25);
        historyArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyArea);
        mainFrame.add(scrollPane, BorderLayout.EAST);
    }

    private static void createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));
        JButton start25 = new JButton("Start 25min");
        JButton start50 = new JButton("Start 50min");
        JButton reset = new JButton("Reset");

        start25.addActionListener(e -> startNewSession(25));
        start50.addActionListener(e -> startNewSession(50));
        reset.addActionListener(e -> resetTimer());

        panel.add(start25);
        panel.add(start50);
        panel.add(reset);
        mainFrame.add(panel, BorderLayout.SOUTH);
    }

    private static void startNewSession(int minutes) {
        if (subjectComboBox.getSelectedItem() == null) {
            showError("Please select or create a subject first!");
            return;
        }
        
        if (studyTimer != null && studyTimer.isRunning()) {
            studyTimer.stop();
        }

        remainingSeconds = minutes * 60;
        updateTimerDisplay();
        
        studyTimer = new Timer(1000, e -> {
            remainingSeconds--;
            updateTimerDisplay();
            
            if (remainingSeconds <= 0) {
                studyTimer.stop();
                saveSessionToDatabase(minutes);
                SwingUtilities.invokeLater(() -> {
                    showInfo("Session complete! Good job!");
                    Toolkit.getDefaultToolkit().beep();
                });
            }
        });
        studyTimer.start();
    }

    private static void updateTimerDisplay() {
        SwingUtilities.invokeLater(() -> {
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
            timerLabel.setForeground(remainingSeconds <= 60 ? Color.RED : new Color(0, 100, 0));
        });
    }

    private static void resetTimer() {
        if (studyTimer != null) studyTimer.stop();
        timerLabel.setText("25:00");
        timerLabel.setForeground(new Color(0, 100, 0));
    }

    private static void showAddSubjectDialog() {
        String subjectName = JOptionPane.showInputDialog(mainFrame, "Enter new subject name:");
        if (subjectName != null && !subjectName.trim().isEmpty()) {
            addSubject(subjectName.trim());
        }
    }

    // Database operations
    private static boolean validateUser(String username, String password) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            return stmt.executeQuery().next();
        }
    }

    private static boolean registerUser(String username, String password) throws SQLException {
        if (getUserId(username) != -1) return false;
        
        try (PreparedStatement stmt = connection.prepareStatement(
            "INSERT INTO users (username, password) VALUES (?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            return true;
        }
    }

    private static int getUserId(String username) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT id FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    private static void addSubject(String subjectName) {
        try {
            int userId = getUserId(currentUser);
            try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO subjects (user_id, name) VALUES (?, ?)")) {
                stmt.setInt(1, userId);
                stmt.setString(2, subjectName);
                stmt.executeUpdate();
                loadSubjects();
                subjectComboBox.setSelectedItem(subjectName);
            }
        } catch (SQLException ex) {
            showError("Subject already exists!");
        }
    }

    private static void loadSubjects() {
        subjectModel.removeAllElements();
        try {
            int userId = getUserId(currentUser);
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM subjects WHERE user_id = ? ORDER BY name")) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    subjectModel.addElement(rs.getString("name"));
                }
            }
        } catch (SQLException ex) {
            showError("Error loading subjects");
        }
    }

    private static void saveSessionToDatabase(int duration) {
        try {
            int userId = getUserId(currentUser);
            String subjectName = (String) subjectComboBox.getSelectedItem();
            int subjectId = getSubjectId(userId, subjectName);
            
            try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO study_sessions (user_id, subject_id, duration) VALUES (?, ?, ?)")) {
                stmt.setInt(1, userId);
                stmt.setInt(2, subjectId);
                stmt.setInt(3, duration);
                stmt.executeUpdate();
                loadSessionHistory();
            }
        } catch (SQLException ex) {
            showError("Error saving session");
        }
    }

    private static int getSubjectId(int userId, String subjectName) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT id FROM subjects WHERE user_id = ? AND name = ?")) {
            stmt.setInt(1, userId);
            stmt.setString(2, subjectName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }

    private static void loadSessionHistory() {
        historyArea.setText("");
        try {
            int userId = getUserId(currentUser);
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT s.start_time, sub.name, s.duration " +
                "FROM study_sessions s " +
                "JOIN subjects sub ON s.subject_id = sub.id " +
                "WHERE s.user_id = ? ORDER BY s.start_time DESC")) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                StringBuilder history = new StringBuilder();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                
                while (rs.next()) {
                    LocalDateTime time = rs.getTimestamp("start_time").toLocalDateTime();
                    history.append(String.format("%s - %s (%d mins)\n",
                        time.format(formatter),
                        rs.getString("name"),
                        rs.getInt("duration")
                    ));
                }
                historyArea.setText(history.toString());
            }
        } catch (SQLException ex) {
            showError("Error loading history");
        }
    }

    // Utility methods
    private static void centerFrame(JFrame frame) {
        frame.setLocationRelativeTo(null);
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static void showInfo(String message) {
        JOptionPane.showMessageDialog(null, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }
}