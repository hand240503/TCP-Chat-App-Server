package com.ndh;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.*;
import java.util.List;

class ServerHandler implements Runnable {
	private Socket clientSocket;
	private PrintWriter out;
	private static List<ServerHandler> clientHandlers;
	private static DBConnect dbConnect;

	public ServerHandler(Socket socket, List<ServerHandler> handlers, DBConnect dbConnect) {
		this.clientSocket = socket;
		this.out = null;
		clientHandlers = handlers;
		ServerHandler.dbConnect = dbConnect;

		try {
			this.out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			System.out.println("Lỗi khi tạo PrintWriter: " + e.getMessage());
		}
	}

	@Override
	public void run() {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				System.out.println("Nhận từ client: " + inputLine);
				handleCommand(inputLine);
			}
		} catch (IOException e) {
			System.out.println("Lỗi: " + e.getMessage());
		} finally {
			closeConnection();
		}
	}

	private void handleCommand(String inputLine) {
		if ("CLOSE".equals(inputLine)) {
			System.out.println("Nhận yêu cầu đóng kết nối từ client: " + clientSocket.getInetAddress() + ":"
					+ clientSocket.getPort());
			sendMessage("CLOSE_ACK: Kết nối sẽ được đóng.");
		} else {
			try {
				JsonObject json = JsonParser.parseString(inputLine).getAsJsonObject();
				String code = json.get("code").getAsString();

				if ("LOGIN".equals(code)) {
					handleLogin(json);
				} else if ("MESSAGE".equals(code)) {
					String message = json.get("data").getAsString();
					System.out.println("Tin nhắn nhận được: " + message);
				} else if ("ALL".equals(code)) {
					String message = json.get("data").getAsString();
					broadcastMessage(message);
				} else {
					sendMessage("INVALID_COMMAND: Lệnh không hợp lệ.");
				}
			} catch (Exception e) {
				sendMessage("INVALID_JSON: Định dạng JSON không hợp lệ.");
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
				sendMessage(buildResponse("LOGIN_SUCCESS", userJson));
			} else {
				sendMessage("LOGIN_FAILED: Tài khoản hoặc mật khẩu không đúng.");
			}
		} else {
			sendMessage("LOGIN_FAILED: Định dạng yêu cầu không đúng.");
		}
	}

	private void broadcastMessage(String message) {
		for (ServerHandler handler : clientHandlers) {
			if (handler.getClientSocket() != clientSocket) {
				handler.sendMessage(message);
			}
		}
	}

	public void sendMessage(String message) {
		out.println(message);
	}

	public Socket getClientSocket() {
		return this.clientSocket;
	}

	private boolean authenticateUser(String username, String password) {
		return "user".equals(username) && "pass".equals(password);
	}

	private void closeConnection() {
		try {
			clientSocket.close();
			clientHandlers.remove(this);
			System.out.println("Kết nối đã được đóng: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
		} catch (IOException e) {
			System.out.println("Không thể đóng socket: " + e.getMessage());
		}
	}

	private String buildResponse(String code, String data) {
		JsonObject response = new JsonObject();
		response.addProperty("code", code);
		response.addProperty("data", data);
		return response.toString();
	}
}
