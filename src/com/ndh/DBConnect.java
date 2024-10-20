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
            System.out.println("Kết nối đến cơ sở dữ liệu thành công.");
        } catch (SQLException e) {
            System.out.println("Lỗi kết nối đến cơ sở dữ liệu: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Driver MySQL không được tìm thấy: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Đã đóng kết nối đến cơ sở dữ liệu.");
            } catch (SQLException e) {
                System.out.println("Lỗi khi đóng kết nối: " + e.getMessage());
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
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Lỗi xác thực người dùng: " + e.getMessage());
        }
        return false;
    }

    public String getByUsername(String username) {
        String query = "SELECT id, username FROM users WHERE username = ?";
        String userJson = null;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                String userName = resultSet.getString("username");
                User user = new User(id, userName, null, null);
                userJson = gson.toJson(user);
            }
        } catch (SQLException e) {
            System.out.println("Lỗi lấy thông tin người dùng: " + e.getMessage());
        }
        return userJson;
    }
}
