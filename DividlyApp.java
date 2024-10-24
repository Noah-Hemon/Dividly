import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.table.DefaultTableModel;

public class DividlyApp {

    public static final String DB_URL = "jdbc:mariadb://localhost:3306/";
    public static final String USER = "root";
    public static final String PASSWORD = "/No18}He33";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dividly - Shared Expenses Manager");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setLayout(new BorderLayout());

            // Set a more appealing look and feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Main menu panel with updated style
            JPanel menuPanel = new JPanel(new GridBagLayout());
            menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            menuPanel.setBackground(new Color(60, 63, 65));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JButton btnAddProject = new JButton("Create New Project");
            JButton btnLoadProject = new JButton("Load Existing Project");

            // Button styling
            btnAddProject.setBackground(new Color(75, 110, 175));
            btnAddProject.setForeground(Color.BLACK);
            btnAddProject.setFont(new Font("Arial", Font.BOLD, 16));
            btnAddProject.setFocusPainted(false);
            btnLoadProject.setBackground(new Color(75, 110, 175));
            btnLoadProject.setForeground(Color.BLACK);
            btnLoadProject.setFont(new Font("Arial", Font.BOLD, 16));
            btnLoadProject.setFocusPainted(false);

            gbc.gridx = 0;
            gbc.gridy = 0;
            menuPanel.add(btnAddProject, gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            menuPanel.add(btnLoadProject, gbc);

            frame.add(menuPanel, BorderLayout.NORTH);

            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            tabbedPane.setBackground(new Color(43, 43, 43));
            tabbedPane.setForeground(Color.BLACK);
            frame.add(tabbedPane, BorderLayout.CENTER);

            btnAddProject.addActionListener(e -> createNewProject(tabbedPane));
            btnLoadProject.addActionListener(e -> loadExistingProject(tabbedPane));

            frame.setVisible(true);
        });
    }

    private static void createNewProject(JTabbedPane tabbedPane) {
        String projectName = JOptionPane.showInputDialog(null, "Enter project name:", "Create New Project", JOptionPane.QUESTION_MESSAGE);
        if (projectName == null || projectName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Project creation canceled.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            String dbName = "dividly_" + projectName;

            // Check if database already exists
            ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE '" + dbName + "'");
            if (rs.next()) {
                int option = JOptionPane.showConfirmDialog(null, "Database already exists. Do you want to delete it?", "Database Exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (option == JOptionPane.YES_OPTION) {
                    stmt.executeUpdate("DROP DATABASE " + dbName);
                } else {
                    JOptionPane.showMessageDialog(null, "Loading existing project instead of creating a new one.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    loadExistingProject(tabbedPane);
                    return;
                }
            }

            // Create new database
            stmt.executeUpdate("CREATE DATABASE " + dbName);
            JOptionPane.showMessageDialog(null, "Database " + dbName + " created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

            // Create tables for participants and transactions
            String participantTableSQL = "CREATE TABLE " + dbName + ".participants (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), balance DECIMAL(10, 2) DEFAULT 0)";
            String transactionTableSQL = "CREATE TABLE " + dbName + ".transactions (id INT PRIMARY KEY AUTO_INCREMENT, participant_id INT, amount DECIMAL(10, 2), currency VARCHAR(10), description VARCHAR(255), transaction_date DATE)";
            stmt.executeUpdate(participantTableSQL);
            stmt.executeUpdate(transactionTableSQL);
            JOptionPane.showMessageDialog(null, "Tables created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

            // Store participants' names
            int participantCount = 0;
            while (participantCount < 4) {
                String participantName = JOptionPane.showInputDialog(null, "Enter participant name (or Cancel to finish):", "Add Participant", JOptionPane.QUESTION_MESSAGE);
                if (participantName == null || participantName.isEmpty()) {
                    break;
                }
                String insertSQL = "INSERT INTO " + dbName + ".participants (name) VALUES (?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    pstmt.setString(1, participantName);
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Participant " + participantName + " added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                participantCount++;
            }

            // Load project after creation
            loadProjectTabs(tabbedPane, dbName);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void loadExistingProject(JTabbedPane tabbedPane) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Get all existing Dividly databases
            ResultSet rs = stmt.executeQuery("SHOW DATABASES LIKE 'dividly_%'");
            JComboBox<String> projectComboBox = new JComboBox<>();
            while (rs.next()) {
                projectComboBox.addItem(rs.getString(1).replace("dividly_", ""));
            }

            if (projectComboBox.getItemCount() == 0) {
                JOptionPane.showMessageDialog(null, "No projects found.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int result = JOptionPane.showConfirmDialog(null, projectComboBox, "Select Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            String projectName = (String) projectComboBox.getSelectedItem();
            String dbName = "dividly_" + projectName;

            loadProjectTabs(tabbedPane, dbName);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void loadProjectTabs(JTabbedPane tabbedPane, String dbName) {
        tabbedPane.removeAll();
        tabbedPane.addTab("Debt Summary", new DebtSummaryPanel(dbName));
        tabbedPane.addTab("Transactions & Manage", new ManageTransactionsPanel(dbName));
        tabbedPane.addTab("Add Transaction", new AddTransactionPanel(dbName, tabbedPane));
    }

    // Placeholder panels
    static class DebtSummaryPanel extends JPanel {
        public DebtSummaryPanel(String dbName) {
            setLayout(new BorderLayout());
            JTextArea textArea = new JTextArea();
            textArea.setBackground(new Color(43, 43, 43));
            textArea.setForeground(Color.WHITE);
            textArea.setFont(new Font("Monospaced", Font.BOLD, 18));
            textArea.setEditable(false);
            try (Connection conn = DriverManager.getConnection(DB_URL + dbName, USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT name, balance FROM participants");
                StringBuilder sb = new StringBuilder("Debt Summary: \n");
                while (rs.next()) {
                    String name = rs.getString("name");
                    double balance = rs.getDouble("balance");
                    sb.append(String.format("%s: %.2f\n", name, balance));
                }
                textArea.setText(sb.toString());
            } catch (Exception ex) {
                textArea.setText("Error: " + ex.getMessage());
            }
            add(new JScrollPane(textArea), BorderLayout.CENTER);
        }
    }
}

class ManageTransactionsPanel extends JPanel {
    private String dbName;
    private JTable transactionTable;

    public ManageTransactionsPanel(String dbName) {
        this.dbName = dbName;
        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Transaction Table styling
        String[] columnNames = {"ID", "Participant ID", "Amount", "Currency", "Description", "Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        transactionTable = new JTable(tableModel);
        transactionTable.setBackground(new Color(43, 43, 43));
        transactionTable.setForeground(Color.WHITE);
        transactionTable.setFont(new Font("Monospaced", Font.PLAIN, 16));
        transactionTable.setRowHeight(30);
        transactionTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        transactionTable.getTableHeader().setBackground(new Color(60, 63, 65));
        transactionTable.getTableHeader().setForeground(Color.BLACK);
        splitPane.setTopComponent(new JScrollPane(transactionTable));

        add(splitPane, BorderLayout.CENTER);
        refreshData();
    }

    private void refreshData() {
        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL + dbName, DividlyApp.USER, DividlyApp.PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM transactions");
            DefaultTableModel tableModel = (DefaultTableModel) transactionTable.getModel();
            tableModel.setRowCount(0); // Clear existing rows

            while (rs.next()) {
                int id = rs.getInt("id");
                int participantId = rs.getInt("participant_id");
                double amount = rs.getDouble("amount");
                String currency = rs.getString("currency");
                String description = rs.getString("description");
                String date = rs.getString("transaction_date");
                tableModel.addRow(new Object[]{id, participantId, amount, currency, description, date});
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class AddTransactionPanel extends JPanel {
    private String dbName;

    public AddTransactionPanel(String dbName, JTabbedPane tabbedPane) {
        this.dbName = dbName;
        setLayout(new GridBagLayout());
        setBackground(new Color(80, 80, 80)); // Darken the panel background to match the theme
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblWhoPays = new JLabel("Who Pays:");
        lblWhoPays.setForeground(Color.BLACK);
        lblWhoPays.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(lblWhoPays, gbc);

        JComboBox<String> comboWhoPays = new JComboBox<>();
        gbc.gridx = 1;
        gbc.gridy = 0;
        comboWhoPays.setFont(new Font("Arial", Font.PLAIN, 16));
        add(comboWhoPays, gbc);

        JLabel lblForWho = new JLabel("For Who:");
        lblForWho.setForeground(Color.BLACK);
        lblForWho.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(lblForWho, gbc);

        JPanel checkboxPanel = new JPanel(new GridLayout(0, 1));
        checkboxPanel.setBackground(new Color(60, 63, 65));
        java.util.List<JCheckBox> checkboxes = new java.util.ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL + dbName, DividlyApp.USER, DividlyApp.PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT name FROM participants");
            while (rs.next()) {
                String name = rs.getString("name");
                comboWhoPays.addItem(name);
                JCheckBox checkBox = new JCheckBox(name);
                checkBox.setForeground(Color.WHITE);
                checkBox.setFont(new Font("Arial", Font.PLAIN, 16));
                checkBox.setBackground(new Color(60, 63, 65));
                checkboxes.add(checkBox);
                checkboxPanel.add(checkBox);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JScrollPane listScroller = new JScrollPane(checkboxPanel);
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(listScroller, gbc);

        JLabel lblAmount = new JLabel("Amount:");
        lblAmount.setForeground(Color.BLACK);
        lblAmount.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(lblAmount, gbc);

        JTextField txtAmount = new JTextField();
        txtAmount.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(txtAmount, gbc);

        JLabel lblCurrency = new JLabel("Currency:");
        lblCurrency.setForeground(Color.BLACK);
        lblCurrency.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(lblCurrency, gbc);

        JComboBox<String> comboCurrency = new JComboBox<>(new String[]{"EUR", "USD", "GBP", "JPY"});
        comboCurrency.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        gbc.gridy = 3;
        add(comboCurrency, gbc);

        JButton btnSubmitTransaction = new JButton("Submit");
        btnSubmitTransaction.setBackground(new Color(75, 110, 175));
        btnSubmitTransaction.setForeground(Color.WHITE);
        btnSubmitTransaction.setFont(new Font("Arial", Font.BOLD, 16));
        btnSubmitTransaction.setFocusPainted(false);
        gbc.gridx = 1;
        gbc.gridy = 4;
        add(btnSubmitTransaction, gbc);

        btnSubmitTransaction.addActionListener(evt -> {
            String whoPays = (String) comboWhoPays.getSelectedItem();
            java.util.List<String> selectedForWho = new java.util.ArrayList<>();
            for (JCheckBox checkbox : checkboxes) {
                if (checkbox.isSelected()) {
                    selectedForWho.add(checkbox.getText());
                }
            }
            double amount;
            try {
                amount = Double.parseDouble(txtAmount.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String currency = (String) comboCurrency.getSelectedItem();
            LocalDate date = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            double splitAmount = amount / (double) selectedForWho.size();

            try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL + dbName, DividlyApp.USER, DividlyApp.PASSWORD)) {
                // Update the payer's balance
                try (PreparedStatement updatePayer = conn.prepareStatement("UPDATE participants SET balance = balance + ? WHERE name = ?")) {
                    updatePayer.setDouble(1, amount);
                    updatePayer.setString(2, whoPays);
                    updatePayer.executeUpdate();
                }
                // Update each receiver's balance and insert the transaction
                for (String forWho : selectedForWho) {
                    try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO transactions (participant_id, amount, currency, description, transaction_date) VALUES ((SELECT id FROM participants WHERE name=?), ?, ?, ?, ?)");
                         PreparedStatement updateReceiver = conn.prepareStatement("UPDATE participants SET balance = balance - ? WHERE name = ?")) {
                        pstmt.setString(1, whoPays);
                        pstmt.setDouble(2, splitAmount);
                        pstmt.setString(3, currency);
                        pstmt.setString(4, "Payment for " + forWho);
                        pstmt.setDate(5, java.sql.Date.valueOf(date));
                        pstmt.executeUpdate();

                        updateReceiver.setDouble(1, splitAmount);
                        updateReceiver.setString(2, forWho);
                        updateReceiver.executeUpdate();
                    }
                }
                // Refresh the tabs to reflect the new data
                DividlyApp.loadProjectTabs(tabbedPane, dbName);
                JOptionPane.showMessageDialog(this, "Transaction added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
