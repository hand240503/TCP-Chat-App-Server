package com.ndh;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;
import com.ndh.model.User;

public class DBConnect {

    private static final String URL = "jdbc:mysql://localhost:3306/chat_app";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final Gson gson = new Gson();

    private Connection connection;

    public DBConnect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            log("DB_CONNECTION_SUCCESS", "Kết nối đến cơ sở dữ liệu thành công.");
        } catch (SQLException e) {
            log("DB_CONNECTION_ERROR", "Lỗi kết nối đến cơ sở dữ liệu: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            log("DB_DRIVER_ERROR", "Driver MySQL không được tìm thấy: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                log("DB_CONNECTION_CLOSE", "Đã đóng kết nối đến cơ sở dữ liệu.");
            } catch (SQLException e) {
                log("DB_CLOSE_CONNECTION_ERROR", "Lỗi khi đóng kết nối: " + e.getMessage());
            }
        }
    }

    public boolean login(String username, String password) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                log("USER_LOGIN", "Người dùng " + username + " đã đăng nhập thành công.");
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log("LOGIN_ERROR", "Lỗi xác thực người dùng: " + e.getMessage());
        }
        return false;
    }

    public String getByUsername(String username) {
        String query = "SELECT id, username, info03 FROM users WHERE username = ?";
        String userJson = null;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                String userName = resultSet.getString("username");
                String avatar = resultSet.getString("info03");
                User user = new User(id, userName, null, avatar);
                userJson = gson.toJson(user);
                log("USER_INFO_FETCH", "Thông tin người dùng cho " + username + " đã được lấy thành công.");
            }
        } catch (SQLException e) {
            log("FETCH_USER_INFO_ERROR", "Lỗi lấy thông tin người dùng: " + e.getMessage());
        }
        return userJson;
    }

    public boolean register(String username, String password, String info01) {
        String sql = "INSERT INTO users (username, password, info01, date_01) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, info01);
            int rowsAffected = pstmt.executeUpdate();
            log("USER_REGISTER", "Người dùng " + username + " đã đăng ký thành công.");
            return rowsAffected > 0;
        } catch (SQLException e) {
            log("REGISTER_ERROR", "Lỗi khi đăng ký người dùng: " + e.getMessage());
            return false;
        }
    }

    private void log(String code, String msg) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String logEntry = String.format("[%s] CODE: %s, MESSAGE: %s", timestamp, code, msg);
        System.out.println(logEntry);
    }
}
