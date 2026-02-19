import java.sql.*;

public class DatabaseConnector {
    private Connection connection;
    private String url;
    private String user;
    private String password;

    public DatabaseConnector(String url, String user, String password) {
        try {
            this.url = url;
            this.user = user;
            this.password = password;
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public Connection getConnection() throws SQLException {
        return this.connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
