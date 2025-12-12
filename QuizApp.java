package QuizApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;

// =========================
// MAIN CLASS (all in one)
// =========================

public class QuizApp extends JFrame {

    // ============ DB SETTINGS ============
    private static final String DB_URL = "jdbc:mysql://localhost:3306/quiz_app";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    // ============ LOGIN COMPONENTS ============
    JTextField usernameField;
    JPasswordField passwordField;
    JButton loginButton;

    // ============ QUIZ COMPONENTS ============
    JLabel questionLabel;
    JRadioButton[] options = new JRadioButton[4];
    ButtonGroup group = new ButtonGroup();
    JButton nextButton;

    int currentIndex = 0;
    int score = 0;

    ArrayList<Integer> questionIDs = new ArrayList<>();
    int loggedUserId = -1;
    String loggedFullName = "";

    // =========================================
    // CONSTRUCTOR - SHOW LOGIN
    // =========================================
    public QuizApp() {
        showLoginScreen();
    }

    // =========================================
    // LOGIN UI
    // =========================================
    private void showLoginScreen() {
        setTitle("Quiz App - Login");
        setSize(350, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1));

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        loginButton = new JButton("Login");

        add(new JLabel("Username:", SwingConstants.CENTER));
        add(usernameField);
        add(new JLabel("Password:", SwingConstants.CENTER));
        add(passwordField);
        add(loginButton);

        loginButton.addActionListener(e -> login());

        setVisible(true);
    }

    // =========================================
    // LOGIN FUNCTION
    // =========================================
    private void login() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());

        try {
            Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            PreparedStatement pst = con.prepareStatement(
                "SELECT user_id, full_name FROM users WHERE username=? AND password=?"
            );
            pst.setString(1, user);
            pst.setString(2, pass);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                loggedUserId = rs.getInt("user_id");
                loggedFullName = rs.getString("full_name");

                JOptionPane.showMessageDialog(this, "Login successful!");

                dispose();
                loadQuizUI();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password!");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
        }
    }

    // =========================================
    // QUIZ UI
    // =========================================
    private void loadQuizUI() {
        setTitle("Online Quiz");
        setSize(600, 350);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(6, 1));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        questionLabel = new JLabel("Question Loading...", SwingConstants.CENTER);
        questionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(questionLabel);

        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton();
            group.add(options[i]);
            add(options[i]);
        }

        nextButton = new JButton("Next");
        add(nextButton);
        nextButton.addActionListener(e -> nextQuestion());

        loadQuestionIDs();
        loadQuestion();

        setVisible(true);
    }

    // =========================================
    // LOAD QUESTION IDS
    // =========================================
    private void loadQuestionIDs() {
        try {
            Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT question_id FROM questions");

            while (rs.next()) {
                questionIDs.add(rs.getInt(1));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================
    // LOAD ONE QUESTION
    // =========================================
    private void loadQuestion() {

        if (currentIndex >= questionIDs.size()) {
            showResultScreen();
            return;
        }

        int qid = questionIDs.get(currentIndex);

        try {
            Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            PreparedStatement pst1 = con.prepareStatement(
                    "SELECT question_text FROM questions WHERE question_id=?"
            );
            pst1.setInt(1, qid);
            ResultSet rs1 = pst1.executeQuery();

            if (rs1.next()) {
                questionLabel.setText("Q" + (currentIndex + 1) + ": " + rs1.getString(1));
            }

            PreparedStatement pst2 = con.prepareStatement(
                    "SELECT option_text, is_correct FROM options WHERE question_id=?"
            );
            pst2.setInt(1, qid);
            ResultSet rs2 = pst2.executeQuery();

            int i = 0;
            while (rs2.next()) {
                options[i].setText(rs2.getString(1));
                options[i].putClientProperty("correct", rs2.getBoolean(2));
                i++;
            }

            group.clearSelection();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================
    // NEXT QUESTION
    // =========================================
    private void nextQuestion() {

        for (JRadioButton opt : options) {
            if (opt.isSelected()) {
                boolean correct = (boolean) opt.getClientProperty("correct");
                if (correct) score++;
            }
        }

        currentIndex++;
        loadQuestion();
    }

    // =========================================
    // FINISH QUIZ AND SHOW RESULT
    // =========================================
    private void showResultScreen() {

        saveResultToDB();

        JOptionPane.showMessageDialog(
                this,
                "Quiz Completed!\nUser: " + loggedFullName +
                        "\nScore: " + score + "/" + questionIDs.size()
        );

        System.exit(0);
    }

    // =========================================
    // SAVE RESULT
    // =========================================
    private void saveResultToDB() {

        try {
            Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO results (user_id, score, total_questions) VALUES (?, ?, ?)"
            );

            pst.setInt(1, loggedUserId);
            pst.setInt(2, score);
            pst.setInt(3, questionIDs.size());
            pst.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================
    // MAIN
    // =========================================
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // load driver
        } catch (Exception e) {
            e.printStackTrace();
        }
        new QuizApp();
    }
}

