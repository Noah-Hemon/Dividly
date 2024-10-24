# Dividly

This project is a Java-based expense management system with a GUI that tracks transactions between participants, allowing users to mark transactions as reimbursed and adjust balances accordingly. It includes a summary panel displaying participant balances and a feature to print a debt summary report indicating who needs to pay whom. The project also dynamically generates a text file summarizing debts, named according to the database in use.

To start this project, you need to modify the DividlyApp class, specifically the PASSWORD field, to match your MariaDB password. This field is located in the line:
public static final String PASSWORD = "your_password";, and you should replace "your_password" with your own database password. Once updated, you can run the application, and it will connect to your MariaDB instance with the correct credentials.
