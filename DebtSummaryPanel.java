import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DebtSummaryPanel extends JPanel {
    private String dbName;

    public DebtSummaryPanel(String dbName) {
        this.dbName = dbName;
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

        JButton printButton = new JButton("Print Text File");
        printButton.setFont(new Font("Arial", Font.BOLD, 16));
        printButton.setBackground(new Color(75, 110, 175));
        printButton.setForeground(Color.BLACK);
        printButton.setFocusPainted(false);

        printButton.addActionListener(e -> printDebtSummary());

        add(printButton, BorderLayout.SOUTH);
        add(new JScrollPane(summaryPanel), BorderLayout.CENTER);
    }

    private void printDebtSummary() {
        List<Participant> participants = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL + dbName, DividlyApp.USER, DividlyApp.PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT name, balance FROM participants");
            while (rs.next()) {
                String name = rs.getString("name");
                double balance = rs.getDouble("balance");
                        participants.add(new Participant(name, balance));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Participant> creditors = new ArrayList<>();
        List<Participant> debtors = new ArrayList<>();

        for (Participant p : participants) {
            if (p.balance > 0) {
                creditors.add(p);
            } else if (p.balance < 0) {
                debtors.add(p);
            }
        }

        String fileName = "debt_summary_" + dbName + ".txt";

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Debt Summary - Who Needs to Pay Whom:\n\n");

            for (Participant debtor : debtors) {
                double remainingDebt = -debtor.balance;
                writer.write(String.format("%s owes:\n", debtor.name));

                for (Participant creditor : creditors) {
                    if (remainingDebt <= 0) break;

                    double payAmount = Math.min(remainingDebt, creditor.balance);
                    if (payAmount > 0) {
                        writer.write(String.format("  - %s: %.2f\n", creditor.name, payAmount));
                        creditor.balance -= payAmount;
                        remainingDebt -= payAmount;
                    }
                }

                writer.write("\n");
            }

            writer.write("End of Debt Summary.\n");
            JOptionPane.showMessageDialog(this, "Debt summary written to '" + fileName + "'!");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writing file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class Participant {
        String name;
        double balance;

        public Participant(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }
    }
}
