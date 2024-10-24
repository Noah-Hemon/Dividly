import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DebtSummaryPanel extends JPanel {
    public DebtSummaryPanel(String dbName) {
        setLayout(new BorderLayout());
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(new Color(43, 43, 43));

        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL + dbName, DividlyApp.USER, DividlyApp.PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT name, balance FROM participants");
            while (rs.next()) {
                String name = rs.getString("name");
                double balance = rs.getDouble("balance");
                JLabel label = new JLabel(String.format("%s: %.2f", name, balance));
                label.setFont(new Font("Monospaced", Font.BOLD, 18));
                if (balance >= 0) {
                    label.setForeground(Color.GREEN);
                } else {
                    label.setForeground(Color.RED);
                }
                summaryPanel.add(label);
            }
        } catch (Exception ex) {
            JLabel errorLabel = new JLabel("Error: " + ex.getMessage());
            errorLabel.setForeground(Color.RED);
            summaryPanel.add(errorLabel);
        }

        add(new JScrollPane(summaryPanel), BorderLayout.CENTER);
    }
}
