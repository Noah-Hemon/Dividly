import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ManageTransactionsPanel extends JPanel {
    private String dbName;
    private JTable transactionTable;
    private DefaultTableModel tableModel;

    public ManageTransactionsPanel(String dbName) {
        this.dbName = dbName;
        setLayout(new BorderLayout());

        String[] columnNames = {"Participant Name", "Amount", "Currency", "Description", "Date", "Reimbursed"};
        tableModel = new DefaultTableModel(columnNames, 0);
        transactionTable = new JTable(tableModel) {
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component comp = super.prepareRenderer(renderer, row, column);
                Boolean reimbursed = (Boolean) getModel().getValueAt(row, 5);
                if (reimbursed != null && reimbursed) {
                    comp.setBackground(Color.GREEN.darker());
                } else {
                    comp.setBackground(Color.BLACK);
                    comp.setForeground(Color.WHITE);
                }
                return comp;
            }
        };

        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setRowSelectionAllowed(true);
        transactionTable.setColumnSelectionAllowed(false);

        transactionTable.setBackground(Color.BLACK);
        transactionTable.setForeground(Color.WHITE);
        transactionTable.setFont(new Font("Monospaced", Font.PLAIN, 16));
        transactionTable.setRowHeight(30);
        transactionTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        transactionTable.getTableHeader().setBackground(Color.WHITE);
        transactionTable.getTableHeader().setForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        add(scrollPane, BorderLayout.CENTER);

        JButton toggleReimbursedButton = new JButton("Toggle Reimbursed Status");
        toggleReimbursedButton.setFont(new Font("Arial", Font.BOLD, 16));
        toggleReimbursedButton.setBackground(new Color(75, 110, 175));
        toggleReimbursedButton.setForeground(Color.BLACK);

        toggleReimbursedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = transactionTable.getSelectedRow();
                if (selectedRow != -1) {
                    boolean reimbursed = (boolean) tableModel.getValueAt(selectedRow, 5);
                    String participantName = tableModel.getValueAt(selectedRow, 0).toString();

                    updateReimbursedStatus(participantName, !reimbursed);
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a transaction to toggle reimbursed status.");
                }
            }
        });

        add(toggleReimbursedButton, BorderLayout.SOUTH);

        refreshData();
    }

    private void refreshData() {
        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL + dbName, DividlyApp.USER, DividlyApp.PASSWORD);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT p.name AS participant_name, t.id AS transaction_id, t.amount, t.currency, t.description, t.transaction_date, t.reimbursed " +
                    "FROM transactions t JOIN participants p ON t.participant_id = p.id");
            tableModel.setRowCount(0);

            while (rs.next()) {
                String participantName = rs.getString("participant_name");
                double amount = rs.getDouble("amount");
                String currency = rs.getString("currency");
                String description = rs.getString("description");
                String date = rs.getString("transaction_date");
                boolean reimbursed = rs.getBoolean("reimbursed");

                tableModel.addRow(new Object[]{participantName, amount, currency, description, date, reimbursed});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateReimbursedStatus(String participantName, boolean reimbursed) {
        try (Connection conn = DriverManager.getConnection(DividlyApp.DB_URL + dbName, DividlyApp.USER, DividlyApp.PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE transactions SET reimbursed = ? WHERE participant_id = (SELECT id FROM participants WHERE name = ?)")) {
            pstmt.setBoolean(1, reimbursed);
            pstmt.setString(2, participantName);
            pstmt.executeUpdate();
            refreshData();
            JOptionPane.showMessageDialog(null, "Transaction status updated successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
