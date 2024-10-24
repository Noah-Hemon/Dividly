import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

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

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            JPanel menuPanel = new JPanel(new GridBagLayout());
            menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            menuPanel.setBackground(new Color(60, 63, 65));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JButton btnAddProject = new JButton("Create New Project");
            JButton btnLoadProject = new JButton("Load Existing Project");

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

            btnAddProject.addActionListener(e -> ProjectManager.createNewProject(tabbedPane));
            btnLoadProject.addActionListener(e -> ProjectManager.loadExistingProject(tabbedPane));

            frame.setVisible(true);
        });
    }
}
