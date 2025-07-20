package desktop.app.desktopassistant.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class H2Example {
    public static void main(String[] args) throws Exception {
        // Connect to H2 in-memory DB
        Connection conn = DriverManager.getConnection("jdbc:h2:./data/mydb", "sa", "");

        Statement stmt = conn.createStatement();

//        // Create table
//        stmt.execute("CREATE TABLE users(id INT PRIMARY KEY, name VARCHAR(255))");
//
//        // Insert data
//        stmt.execute("INSERT INTO users VALUES(1, 'Alice'), (2, 'Bob')");

        // Query data
        ResultSet rs = stmt.executeQuery("SELECT * FROM users");

        while (rs.next()) {
            System.out.println(rs.getInt("id") + " => " + rs.getString("name"));
        }

        conn.close();
    }
}

