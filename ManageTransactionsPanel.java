import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ManageTransactionsPanel extends JPanel {
    private String dbName;
    private JTable transactionTable;

    public ManageTransactionsPanel(String dbName) {
        this.dbName = dbName;
        setLayout(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        String[] columnNames = {"Participant Name", "Amount", "Currency", "Description", "Date"};
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
            ResultSet rs = stmt.executeQuery("SELECT p.name AS participant_name, t.amount, t.currency, t.description, t.transaction_date FROM transactions t JOIN participants p ON t.participant_id = p.id");
            DefaultTableModel tableModel = (DefaultTableModel) transactionTable.getModel();
            tableModel.setRowCount(0);

            while (rs.next()) {
                String participantName = rs.getString("participant_name");
                double amount = rs.getDouble("amount");
                String currency = rs.getString("currency");
                String description = rs.getString("description");
                String date = rs.getString("transaction_date");
                tableModel.addRow(new Object[]{participantName, amount, currency, description, date});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
