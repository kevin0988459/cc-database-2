package edu.cmu.cc.minisite;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

/**
 * Task 1: This query simulates the login process of a user and tests whether
 * your backend system is functioning properly. Your web application will
 * receive a pair of UserName and Password, and you need to check your backend
 * database to see if the UserName and Password is a valid pair. You should
 * construct your response accordingly:
 *
 * If YES, send back the userName and Profile Image URL. If NOT, set userName as
 * "Unauthorized" and Profile Image URL as "#".
 */
public class ProfileServlet extends HttpServlet {

    /**
     * JDBC driver of MySQL Connector/J.
     */
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    /**
     * Database name.
     */
    private static final String DB_NAME = "reddit_db";

    /**
     * The endpoint of the database.
     *
     * To avoid hardcoding credentials, use environment variables to include the
     * credentials.
     *
     * e.g., before running "mvn clean package exec:java" to start the server
     * run the following commands to set the environment variables. export
     * MYSQL_HOST=... export MYSQL_NAME=... export MYSQL_PWD=...
     */
    private static String mysqlHost = System.getenv("MYSQL_HOST");
    /**
     * MySQL username.
     */
    private static String mysqlName = System.getenv("MYSQL_NAME");
    /**
     * MySQL Password.
     */
    private static String mysqlPwd = System.getenv("MYSQL_PWD");

    /**
     * The connection (session) with the database. HINT: pay attention to how
     * this is used internally
     */
    private static Connection conn;

    /**
     * MySQL URL.
     */
    private static final String URL = "jdbc:mysql://" + mysqlHost + ":3306/"
            + DB_NAME + "?useSSL=false&serverTimezone=UTC";

    /**
     * Initialize SQL connection. Standard constructor
     *
     * @throws ClassNotFoundException when an application fails to load a class
     * @throws SQLException on a database access error or other errors
     */
    public ProfileServlet() throws ClassNotFoundException, SQLException {
        conn = getDBConnection();
    }

    /**
     * A special constructor for TDD
     *
     * @param conn The connection to use
     */
    ProfileServlet(Connection conn) {
        ProfileServlet.conn = conn;
    }

    private Connection getDBConnection() throws SQLException {
        Objects.requireNonNull(mysqlHost);
        Objects.requireNonNull(mysqlName);
        Objects.requireNonNull(mysqlPwd);
        return DriverManager.getConnection(URL, mysqlName, mysqlPwd);
    }

    /**
     * Method that handles HttpServletRequests (GET)
     *
     * @param request the request object that is passed to the servlet
     * @param response the response object that the servlet uses to return the
     * headers to the client
     * @throws IOException if an input or output error occurs
     */
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        String name = request.getParameter("id");
        String pwd = request.getParameter("pwd");
        JsonObject result = validateLoginAndReturnResult(name, pwd);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write(result.toString());
        writer.close();
    }

    /**
     * Method to perform the SQL query, retrieve the results and construct and
     * return a JsonObject with the expected result
     *
     * @param name The username supplied via the HttpServletRequest
     * @param pwd The password supplied via the HttpServletRequest
     * @return A JsonObject with the servlet's response
     */
    JsonObject validateLoginAndReturnResult(String name, String pwd) {
        JsonObject result = new JsonObject();
        String q = "SELECT username, profile_photo_url FROM users WHERE username = ? AND pwd = ?";
        // send the query to the database
        try (PreparedStatement stmt = conn.prepareStatement(q)) {
            stmt.setString(1, name);
            stmt.setString(2, pwd);
            ResultSet rs = stmt.executeQuery();
            // result -> {"username": "...", "profile_photo_url": "..."}
            if (rs.next()) {
                result.addProperty("name", rs.getString("username"));
                result.addProperty("profile", rs.getString("profile_photo_url"));
            } else {
                result.addProperty("name", "Unauthorized");
                result.addProperty("profile", "#");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Retrieves the user's profile information.(Task4)
     *
     * @param name Username.
     * @return profile
     */
    public String getProfile(String name) {
        String result = "#";
        String query = "SELECT username, profile_photo_url FROM users WHERE username = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getString("profile_photo_url");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Closes the database connection.
     */
    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
