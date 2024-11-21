package com.ndh;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.ndh.model.Conversation;
import com.ndh.model.Message;
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
		String query = "SELECT id, username, info01, info03 FROM users WHERE username = ?";
		String userJson = null;

		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setString(1, username);
			ResultSet resultSet = statement.executeQuery();

			if (resultSet.next()) {
				int id = resultSet.getInt("id");
				String userName = resultSet.getString("username");
				String info01 = resultSet.getString("info01");
				String avatar = resultSet.getString("info03");
				User user = new User(id, userName, null, info01, null, avatar);
				userJson = gson.toJson(user);
				log("USER_INFO_FETCH", "Thông tin người dùng cho " + username + " đã được lấy thành công.");
			}
		} catch (SQLException e) {
			log("FETCH_USER_INFO_ERROR", "Lỗi lấy thông tin người dùng: " + e.getMessage());
		}
		return userJson;
	}

	public boolean register(String username, String password, String info01) {
	    String sql = "INSERT INTO users (username, password, info01, info03,created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())";

	    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
	        pstmt.setString(1, username);
	        pstmt.setString(2, password);
	        pstmt.setString(3, info01);
	        pstmt.setString(4, "files\\avatar.png");
	        int rowsAffected = pstmt.executeUpdate();
	        log("USER_REGISTER", "Người dùng " + username + " đã đăng ký thành công.");
	        return rowsAffected > 0;
	    } catch (SQLException e) {
	        log("REGISTER_ERROR", "Lỗi khi đăng ký người dùng: " + e.getMessage());
	        return false;
	    }
	}


	public String getListUserAsJson(int id) {
		List<User> lst = new ArrayList<>();

		String sql = "SELECT * FROM users u WHERE u.id != ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, id);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					User user = new User();
					user.setId(rs.getInt("id"));
					user.setUsername(rs.getString("username"));
					user.setInfo01(rs.getString("info01"));
					user.setAvatar(rs.getString("info03"));

					lst.add(user);
				}
			}
		} catch (SQLException e) {
			log("FETCH_USER_LIST_ERROR", "Lỗi lấy danh sách người dùng: " + e.getMessage());
		}

		return gson.toJson(lst);
	}

	public List<User> getListUserAsList(int id) {
		List<User> lst = new ArrayList<>();

		String sql = "SELECT * FROM users u WHERE u.id != ?";
		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, id);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					User user = new User();
					user.setId(rs.getInt("id"));
					user.setUsername(rs.getString("username"));
					user.setInfo01(rs.getString("info01"));
					user.setAvatar(rs.getString("info03"));

					lst.add(user);
				}
			}
		} catch (SQLException e) {
			log("FETCH_USER_LIST_ERROR", "Lỗi lấy danh sách người dùng: " + e.getMessage());
		}

		return lst;
	}

	public Conversation addConversation(String title, String code) {
		String sql = "INSERT INTO conversation (title, code, created_at, updated_at) VALUES (?, ?, current_timestamp(), current_timestamp())";
		Conversation con = null;

		try (PreparedStatement pstmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, title);
			pstmt.setString(2, code);

			int rowsAffected = pstmt.executeUpdate();
			if (rowsAffected > 0) {
				try (ResultSet rs = pstmt.getGeneratedKeys()) {
					if (rs.next()) {
						int generatedId = rs.getInt(1);
						con = new Conversation(generatedId, title, code, null, null);
					}
				}
				log("ADD_CONVERSATION", "Cuộc hội thoại mới đã được thêm thành công với mã: " + code);
			}
		} catch (SQLException e) {
			log("ADD_CONVERSATION_ERROR", "Lỗi khi thêm cuộc hội thoại: " + e.getMessage());
		}

		return con;
	}

	public boolean addParticipant(int conversationId, int userId, int type) {
		String sql = "INSERT INTO participants (conversation_id, user_id, type, created_at, updated_at) VALUES (?, ?, ?, current_timestamp(), current_timestamp())";

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, conversationId);
			pstmt.setInt(2, userId);
			pstmt.setInt(3, type);

			int rowsAffected = pstmt.executeUpdate();
			log("ADD_PARTICIPANT",
					"Người tham gia mới đã được thêm vào cuộc hội thoại " + conversationId + " với user_id: " + userId);
			return rowsAffected > 0;
		} catch (SQLException e) {
			log("ADD_PARTICIPANT_ERROR", "Lỗi khi thêm người tham gia vào cuộc hội thoại: " + e.getMessage());
			return false;
		}
	}

	public List<Conversation> getConversations() {
		List<Conversation> conversations = new ArrayList<>();
		String sql = "SELECT id, creator_id, title, code, created_at, updated_at FROM conversation";

		try (PreparedStatement pstmt = connection.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {

			while (rs.next()) {
				int id = rs.getInt("id");
				Integer creatorId = rs.getObject("creator_id") != null ? rs.getInt("creator_id") : null;
				String title = rs.getString("title");
				String code = rs.getString("code");
				Timestamp createdAt = rs.getTimestamp("created_at");
				Timestamp updatedAt = rs.getTimestamp("updated_at");

				Conversation conversation = new Conversation(id, title, code, createdAt, updatedAt);
				conversations.add(conversation);
			}
			log("FETCH_CONVERSATIONS", "Lấy danh sách cuộc hội thoại thành công.");
		} catch (SQLException e) {
			log("FETCH_CONVERSATIONS_ERROR", "Lỗi khi lấy danh sách cuộc hội thoại: " + e.getMessage());
		}

		return conversations;
	}

	public Conversation getConversationById(int id) {
		String sql = "SELECT id, title, code, created_at, updated_at FROM conversation WHERE id = ?";
		Conversation conversation = null;

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				int conversationId = rs.getInt("id");
				String title = rs.getString("title");
				String code = rs.getString("code");
				Timestamp createdAt = rs.getTimestamp("created_at");
				Timestamp updatedAt = rs.getTimestamp("updated_at");

				conversation = new Conversation(conversationId, title, code, createdAt, updatedAt);
				log("FETCH_CONVERSATION_BY_ID", "Lấy cuộc hội thoại thành công với ID: " + id);
			} else {
				log("FETCH_CONVERSATION_NOT_FOUND", "Không tìm thấy cuộc hội thoại với ID: " + id);
			}
		} catch (SQLException e) {
			log("FETCH_CONVERSATION_ERROR", "Lỗi khi lấy cuộc hội thoại với ID: " + id + " - " + e.getMessage());
		}

		return conversation;
	}

	public Conversation getConversationByCode(String code) {
		String sql = "SELECT id, title, code, created_at, updated_at FROM conversation WHERE code = ?";
		Conversation conversation = null;

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setString(1, code);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				int conversationId = rs.getInt("id");
				String title = rs.getString("title");
				String conversationCode = rs.getString("code");
				Timestamp createdAt = rs.getTimestamp("created_at");
				Timestamp updatedAt = rs.getTimestamp("updated_at");

				conversation = new Conversation(conversationId, title, conversationCode, createdAt, updatedAt);
				log("FETCH_CONVERSATION_BY_CODE", "Lấy cuộc hội thoại thành công với mã: " + code);
			} else {
				log("FETCH_CONVERSATION_NOT_FOUND", "Không tìm thấy cuộc hội thoại với mã: " + code);
			}
		} catch (SQLException e) {
			log("FETCH_CONVERSATION_ERROR", "Lỗi khi lấy cuộc hội thoại với mã: " + code + " - " + e.getMessage());
		}

		return conversation;
	}

	public void insertMessage(int conversationId, int senderId, int type, String message, String info01) {
		String sql = "INSERT INTO chat_app.message (conversation_id, sender_id, type, message, info01, created_at, updated_at) "
				+ "VALUES (?, ?, ?, ?, ?, current_timestamp(), current_timestamp())";

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, conversationId);
			pstmt.setInt(2, senderId);
			pstmt.setInt(3, type);
			pstmt.setString(4, message);
			pstmt.setString(5, info01);

			pstmt.executeUpdate();
		} catch (SQLException e) {

			System.err.println("Lỗi khi chèn tin nhắn: " + e.getMessage());
		}
	}

	public List<Message> getLstMessage(int conversationId) {
		List<Message> lst = new ArrayList<>();
		String sql = "SELECT id, conversation_id, sender_id, `type`, message, info01, created_at, updated_at FROM chat_app.message WHERE conversation_id = ? ORDER BY created_at ASC";

		try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
			pstmt.setInt(1, conversationId);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				int id = rs.getInt("id");
				int senderId = rs.getInt("sender_id");
				int type = rs.getInt("type");
				String messageContent = rs.getString("message");
				String info01 = rs.getString("info01");
				Timestamp createdAt = rs.getTimestamp("created_at");
				Timestamp updatedAt = rs.getTimestamp("updated_at");

				Message message = new Message(id, conversationId, senderId, type, messageContent, info01, createdAt,
						updatedAt);
				lst.add(message);
			}
			log("FETCH_MESSAGES_BY_CONVERSATION_ID",
					"Lấy danh sách tin nhắn thành công cho conversation_id: " + conversationId);
		} catch (SQLException e) {
			log("FETCH_MESSAGES_ERROR",
					"Lỗi khi lấy danh sách tin nhắn cho conversation_id: " + conversationId + " - " + e.getMessage());
		}

		return lst;
	}

	private void log(String code, String msg) {
		String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		String logEntry = String.format("[%s] CODE: %s, MESSAGE: %s", timestamp, code, msg);
		System.out.println(logEntry);
	}
}
