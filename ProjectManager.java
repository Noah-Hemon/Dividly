import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ProjectManager {
    public static void createNewProject(JTabbedPane tabbedPane) {
        String projectName = JOptionPane.showInputDialog(null, "Enter project name:", "Create New Project", JOptionPane.QUESTION_MESSAGE);
        if (projectName == null || projectName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Project creation canceled.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL, DividlyApp.USER, DividlyApp.PASSWORD);
             Statement stmt = conn.createStatement()) {
            String dbName = "dividly_" + projectName;

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

            stmt.executeUpdate("CREATE DATABASE " + dbName);
            JOptionPane.showMessageDialog(null, "Database " + dbName + " created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

            String participantTableSQL = "CREATE TABLE " + dbName + ".participants (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), balance DECIMAL(10, 2) DEFAULT 0)";
            String transactionTableSQL = "CREATE TABLE " + dbName + ".transactions (id INT PRIMARY KEY AUTO_INCREMENT, participant_id INT, amount DECIMAL(10, 2), currency VARCHAR(10), description VARCHAR(255), transaction_date DATE, reimbursed BOOLEAN DEFAULT FALSE)";
            stmt.executeUpdate(participantTableSQL);
            stmt.executeUpdate(transactionTableSQL);
            JOptionPane.showMessageDialog(null, "Tables created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

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

            loadProjectTabs(tabbedPane, dbName);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void loadExistingProject(JTabbedPane tabbedPane) {
        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL, DividlyApp.USER, DividlyApp.PASSWORD);
             Statement stmt = conn.createStatement()) {

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
}
