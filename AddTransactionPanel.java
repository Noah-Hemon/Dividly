import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddTransactionPanel extends JPanel {
    private String dbName;

    public AddTransactionPanel(String dbName, JTabbedPane tabbedPane) {
        this.dbName = dbName;
        setLayout(new GridBagLayout());
        setBackground(new Color(80, 80, 80));
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
        List<JCheckBox> checkboxes = new ArrayList<>();

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
        btnSubmitTransaction.setForeground(Color.BLACK);
        btnSubmitTransaction.setFont(new Font("Arial", Font.BOLD, 16));
        btnSubmitTransaction.setFocusPainted(false);
        gbc.gridx = 1;
        gbc.gridy = 4;
        add(btnSubmitTransaction, gbc);

        btnSubmitTransaction.addActionListener(evt -> {
            String whoPays = (String) comboWhoPays.getSelectedItem();
            List<String> selectedForWho = new ArrayList<>();
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
                try (PreparedStatement updatePayer = conn.prepareStatement("UPDATE participants SET balance = balance + ? WHERE name = ?")) {
                    updatePayer.setDouble(1, amount);
                    updatePayer.setString(2, whoPays);
                    updatePayer.executeUpdate();
                }
                for (String forWho : selectedForWho) {
                    try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO transactions (participant_id, amount, currency, description, transaction_date, reimbursed) VALUES ((SELECT id FROM participants WHERE name=?), ?, ?, ?, ?, FALSE)");
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
                ProjectManager.loadProjectTabs(tabbedPane, dbName);
                JOptionPane.showMessageDialog(this, "Transaction added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
