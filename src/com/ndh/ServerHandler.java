package com.ndh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.ndh.model.User;
import com.ndh.request.Request;

class ServerHandler implements Runnable {
	private Socket clientSocket;
	private DataOutputStream dataOut;
	private DataInputStream dataIn;
	private static List<ServerHandler> clientHandlers;
	private static DBConnect dbConnect;
	private static Gson gson;

	public ServerHandler(Socket socket, List<ServerHandler> handlers, DBConnect dbConnect) {
		this.clientSocket = socket;
		clientHandlers = handlers;
		ServerHandler.dbConnect = dbConnect;
		gson = new Gson();

		try {
			this.dataOut = new DataOutputStream(clientSocket.getOutputStream());
			this.dataIn = new DataInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			log("ERROR", "Lỗi khi tạo DataOutputStream hoặc DataInputStream: " + e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				String inputLine = dataIn.readUTF();
				log("RECEIVE", "Nhận từ client: " + inputLine);
				handleCommand(inputLine);
			}
		} catch (IOException e) {
			log("ERROR", "Lỗi: " + e.getMessage());
		} finally {
			closeConnection();
		}
	}

	private void handleCommand(String inputLine) {
		if ("CLOSE".equals(inputLine)) {
			log("CLOSE_REQUEST", "Nhận yêu cầu đóng kết nối từ client: " + clientSocket.getInetAddress() + ":"
					+ clientSocket.getPort());
			sendMessage("CLOSE_ACK", "Kết nối sẽ được đóng.");
		} else {
			try {
				JsonObject json = JsonParser.parseString(inputLine).getAsJsonObject();
				String code = json.get("code").getAsString();
				log("COMMAND", "Nhận lệnh: " + code);

				switch (code) {
				case "LOGIN":
					handleLogin(json);
					break;
				case "MESSAGE":
					String message = json.get("data").getAsString();
					log("MESSAGE_RECEIVED", "Tin nhắn nhận được: " + message);
					break;
				case "ALL":
					message = json.get("data").getAsString();
					broadcastMessage(message);
					break;
				case "FILE":
//                        sendAvatarToClient("");
					break;
				case "REGISTER":
					handleRegister(json);
					break;
				default:
					sendMessage("INVALID_COMMAND", "Lệnh không hợp lệ.");
					break;
				}
			} catch (Exception e) {
				sendMessage("INVALID_JSON", "Định dạng JSON không hợp lệ.");
				log("ERROR", "Lỗi xử lý lệnh: " + e.getMessage());
			}
		}
	}

	private void handleLogin(JsonObject json) {
	    if (json.has("data")) {
	        JsonObject data = json.getAsJsonObject("data");
	        String username = data.get("username").getAsString();
	        String password = data.get("password").getAsString();

	        if (dbConnect.login(username, password)) {
	            String userJson = dbConnect.getByUsername(username);
	            User user = gson.fromJson(userJson, User.class);
	            String lst = dbConnect.getListUserAsJson(user.getId());
	            Type userListType = new TypeToken<List<User>>() {}.getType();

	            // Gửi thông tin người dùng và avatar
	            sendMessage("USER_INFO", userJson);
	            sendAvatarToClient(user.getAvatar(), user.getId());

	            // Xử lý danh sách người dùng
	            List<User> userList = gson.fromJson(lst, userListType);
	            sendMessage("LST_USER_INFO", lst);
	            for (User u : userList) {
	                sendAvatarToClient(u.getAvatar(), u.getId());
	            }

	            // Gửi thông báo đăng nhập thành công
	            sendMessage("LOGIN_SUCCESS", null);
	            log("LOGIN_SUCCESS", "Đăng nhập thành công cho người dùng: " + username);
	        } else {
	            sendMessage("LOGIN_FAILED", "Tài khoản hoặc mật khẩu không đúng.");
	            log("LOGIN_FAILED", "Đăng nhập thất bại cho người dùng: " + username);
	        }
	    } else {
	        sendMessage("LOGIN_FAILED", "Định dạng yêu cầu không đúng.");
	        log("LOGIN_FAILED", "Định dạng yêu cầu không đúng.");
	    }
	}


	private void handleRegister(JsonObject json) {
		if (json.has("data")) {
			JsonObject data = json.getAsJsonObject("data");
			String username = data.get("username").getAsString();
			String password = data.get("password").getAsString();
			String info01 = data.get("info01").getAsString();
			if (dbConnect.register(username, password, info01)) {
				sendMessage("REGISTER_SUCCESS", "Đăng ký thành công.");
				log("REGISTER_SUCCESS", "Đăng ký thành công cho người dùng: " + username);
			} else {
				sendMessage("REGISTER_FAILED", "Tài khoản đã tồn tại.");
				log("REGISTER_FAILED", "Tài khoản đã tồn tại: " + username);
			}
		} else {
			sendMessage("REGISTER_FAILED", "Định dạng yêu cầu không đúng.");
			log("REGISTER_FAILED", "Định dạng yêu cầu không đúng.");
		}
	}

	private void broadcastMessage(String message) {
		for (ServerHandler handler : clientHandlers) {
			if (handler.getClientSocket() != clientSocket) {
				handler.sendMessage("MESSAGE", message);
			}
		}
		log("BROADCAST", "Tin nhắn đã được phát đi: " + message);
	}

	public void sendMessage(String code, Object data) {
		Request request = new Request();
		request.setCode(code);
		request.setData(data);
		String jsonResponse = gson.toJson(request);

		try {
			dataOut.writeUTF(jsonResponse);
			dataOut.flush();
			log("SEND", "Gửi phản hồi: " + jsonResponse);
		} catch (IOException e) {
			log("ERROR", "Lỗi khi gửi tin nhắn: " + e.getMessage());
		}
	}

	public Socket getClientSocket() {
		return this.clientSocket;
	}

	private void closeConnection() {
		try {
			clientSocket.close();
			clientHandlers.remove(this);
			log("CLOSE_CONNECTION",
					"Kết nối đã được đóng: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
		} catch (IOException e) {
			log("ERROR", "Không thể đóng socket: " + e.getMessage());
		}
	}

	private void sendAvatarToClient(String filePath, int id) {
		File file = new File(filePath);
		if (!file.exists()) {
			sendMessage("FILE_NOT_FOUND", "File không tồn tại.");
			log("FILE_NOT_FOUND", "File không tồn tại: " + filePath);
			return;
		}

		JsonObject fileInfoJson = new JsonObject();
		fileInfoJson.addProperty("id", id);
		fileInfoJson.addProperty("fileName", file.getName());
		fileInfoJson.addProperty("fileSize", file.length());

		sendMessage("AVATAR", fileInfoJson);
		log("AVATAR_SENT", "Đang gửi file avatar: " + file.getName());

		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			long totalBytesSent = 0;

			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				dataOut.write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;

				log("FILE_PROGRESS", "Đã gửi " + bytesRead + " byte(s), tổng cộng: " + totalBytesSent + " byte(s).");
			}
			dataOut.flush();
			log("FILE_SENT", "File đã được gửi thành công: " + file.getName());
		} catch (IOException e) {
			sendMessage("FILE_TRANSFER_ERROR", "Lỗi trong quá trình gửi file.");
			log("ERROR", "Lỗi khi gửi file: " + e.getMessage());
		}
	}

	private void log(String code, String msg) {
		String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		String logEntry = String.format("[%s] CODE: %s, MESSAGE: %s", timestamp, code, msg);
		System.out.println(logEntry);
	}
}
