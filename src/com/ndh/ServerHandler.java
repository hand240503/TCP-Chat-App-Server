package com.ndh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ndh.model.Conversation;
import com.ndh.model.Message;
import com.ndh.model.SendMessageRequest;
import com.ndh.model.User;
import com.ndh.request.Request;

class ServerHandler implements Runnable {
	private Socket clientSocket;
	private DataOutputStream dataOut;
	private DataInputStream dataIn;
	private static List<ServerHandler> clientHandlers;
	private static Map<String, List<ServerHandler>> usernameToHandlers = null;
	private static DBConnect dbConnect;
	private static Gson gson;
	private User currentUser;

	public ServerHandler(Socket socket, List<ServerHandler> handlers,
			Map<String, List<ServerHandler>> usernameToHandlers, DBConnect dbConnect) {
		this.clientSocket = socket;
		clientHandlers = handlers;
		ServerHandler.usernameToHandlers = usernameToHandlers;
		ServerHandler.dbConnect = dbConnect;
		gson = new Gson();

		try {
			this.dataOut = new DataOutputStream(clientSocket.getOutputStream());
			this.dataIn = new DataInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			log("ERROR", "Lỗi khi tạo DataOutputStream hoặc DataInputStream: " + e.getMessage());
		}
	}

	public synchronized void addClient(String username, ServerHandler handler) {
		usernameToHandlers.computeIfAbsent(username, k -> new ArrayList<>()).add(handler);
	}

	public synchronized boolean isOnline(String username) {
		return usernameToHandlers.containsKey(username) && !usernameToHandlers.get(username).isEmpty();
	}

//	public static List<String> getConnectedClients() {
//		List<String> connectedClients = new ArrayList<>();
//		System.out.println(clientHandlers.size());
//		for (ServerHandler handler : clientHandlers) {
//			if (handler.getClientSocket() != null && !handler.getClientSocket().isClosed()) {
//				String clientInfo = "Client " + handler.getClientSocket().getInetAddress() + ":"
//						+ handler.getClientSocket().getPort();
//				connectedClients.add(clientInfo);
//			}
//		}
//		return connectedClients;
//	}

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
			closeConnection();
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
					JsonObject dataObject = json.getAsJsonObject("data");
					SendMessageRequest msg = gson.fromJson(dataObject, SendMessageRequest.class);
					User userSender = msg.getUserSender();
					User userReceive = msg.getUserReceive();
					log("MESSAGE_RECEIVED", "Tin nhắn nhận được: " + msg);
					String mess = msg.getMessageText();
					dbConnect.insertMessage(msg.getConversation().getId(), userSender.getId(), 1, mess, null);
					if (isOnline(userSender.getUsername())) {
						List<ServerHandler> senderHandlers = usernameToHandlers.get(userSender.getUsername());

						for (ServerHandler handler : senderHandlers) {
							if (handler != this) {
								handler.sendMessage("MESSAGE", msg);
							}
						}
					}

					if (isOnline(userReceive.getUsername())) {
						List<ServerHandler> receiverHandlers = usernameToHandlers.get(userReceive.getUsername());
						for (ServerHandler handler : receiverHandlers) {
							handler.sendMessage("MESSAGE", msg);
						}
					}
					break;
				case "ALL":

					break;
				case "MES-FILE":
					JsonObject userReceiveObject = json.getAsJsonObject("data");
					SendMessageRequest msgRequest = gson.fromJson(userReceiveObject, SendMessageRequest.class);
					handleFileTransfer(msgRequest);
					break;
				case "REGISTER":
					handleRegister(json);
					break;

				case "GETGROUP2USER":
					JsonObject msgObject = json.getAsJsonObject("data");
					SendMessageRequest msgReq = gson.fromJson(msgObject, SendMessageRequest.class);
					handleGetGroupTwoUser(msgReq);
					break;
				case "GET-MES-FILE":
					JsonObject msgFileObject = json.getAsJsonObject("data");
					Message message = gson.fromJson(msgFileObject, Message.class);
					loadImageFileToClient(message.getInfo01(), message.getMessage());
					break;
				case "GET-MES-FILE-ASYN":
					JsonObject msgFileObjectASYN = json.getAsJsonObject("data");
					Message messageAsyn = gson.fromJson(msgFileObjectASYN, Message.class);
					loadFileToClientAsyn(messageAsyn.getInfo01());
					break;
				case "GET-FILE":
					String dataJsonString = json.get("data").getAsString();
					JsonObject dataJson = JsonParser.parseString(dataJsonString).getAsJsonObject();

					String filename = dataJson.get("filename").getAsString();
					String filePath = "files\\" + filename;
					sendFileToClient(filePath);
					break;
				case "MES-AUDIO":
					JsonObject mesAudio = json.getAsJsonObject("data");
					SendMessageRequest mesAudioReq = gson.fromJson(mesAudio, SendMessageRequest.class);
					handleChatAudio(mesAudioReq);
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

	private void handleChatAudio(SendMessageRequest msg) {
		User userSender = msg.getUserSender();
		User userReceive = msg.getUserReceive();
		if (isOnline(userReceive.getUsername())) {
			List<ServerHandler> senderHandlers = usernameToHandlers.get(userSender.getUsername());

			for (ServerHandler handler : senderHandlers) {
				handler.sendMessage("MES-AUDIO-ON", msg);
			}
		}

		if (isOnline(userReceive.getUsername())) {
			List<ServerHandler> receiverHandlers = usernameToHandlers.get(userReceive.getUsername());
			for (ServerHandler handler : receiverHandlers) {
				handler.sendMessage("MES-AUDIO-ON", msg);
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
				currentUser = gson.fromJson(userJson, User.class);

				sendMessage("USER_INFO", userJson);
				sendAvatarToClient(currentUser.getAvatar(), currentUser.getId());

				List<User> lst = dbConnect.getListUserAsList(currentUser.getId());
				for (User u : lst) {
					u.setOnline(isOnline(u.getUsername()));
				}
				String lstJson = gson.toJson(lst);
				sendMessage("LST_USER_INFO", lstJson);
				for (User u : lst) {
					sendAvatarToClient(u.getAvatar(), u.getId());
				}

				addClient(username, this);
				notifyUserIsOnline(currentUser.getUsername());

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

	private void broadcastMessage(String code, String message) {
		for (Map.Entry<String, List<ServerHandler>> entry : usernameToHandlers.entrySet()) {
			List<ServerHandler> handlers = entry.getValue();
			for (ServerHandler handler : handlers) {
				if (handler != this) {
					handler.sendMessage(code, message);
				}
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

	public synchronized void printConnectedUsers() {
		if (usernameToHandlers.isEmpty()) {
			System.out.println("Không có người dùng nào đang kết nối đến server.");
		} else {
			System.out.println("Danh sách người dùng đang kết nối đến server:");
			for (String username : usernameToHandlers.keySet()) {
				System.out.println(" - " + username);
			}
		}
	}

	private void closeConnection() {
		try {
			clientSocket.close();

			synchronized (clientHandlers) {
				clientHandlers.remove(this);
			}

			synchronized (usernameToHandlers) {
				if (currentUser.getUsername() != null && usernameToHandlers.containsKey(currentUser.getUsername())) {
					List<ServerHandler> handlers = usernameToHandlers.get(currentUser.getUsername());
					handlers.remove(this);
					if (handlers.isEmpty()) {
						usernameToHandlers.remove(currentUser.getUsername());
						notifyUserIsOnline(currentUser.getUsername());
					}
				}
			}

			log("CLOSE_CONNECTION",
					"Kết nối đã được đóng: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
			printConnectedUsers();
		} catch (IOException e) {
			log("ERROR", "Không thể đóng socket: " + e.getMessage());
		}
	}

	public static ServerHandler findHandlerByUser(User user) {
		for (ServerHandler handler : clientHandlers) {
			if (handler.currentUser != null && handler.currentUser.getId() == user.getId()) {
				return handler;
			}
		}
		return null;
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
			}
			dataOut.flush();
			log("FILE_SENT", "File đã được gửi thành công: " + file.getName());
		} catch (IOException e) {
			log("ERROR", "Lỗi khi gửi file: " + e.getMessage());
		}
	}

	private void sendFileToClient(String filePath, String mes) {
		File file = new File(filePath);
		if (!file.exists()) {
			sendMessage("FILE_NOT_FOUND", "File không tồn tại.");
			log("FILE_NOT_FOUND", "File không tồn tại: " + filePath);
			return;
		}

		JsonObject fileInfoJson = new JsonObject();
		fileInfoJson.addProperty("fileName", file.getName());
		fileInfoJson.addProperty("fileSize", file.length());
		fileInfoJson.addProperty("fileDes", mes);
		sendMessage("MESSAGE-FILE", fileInfoJson);
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			long totalBytesSent = 0;

			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				dataOut.write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;
			}
			dataOut.flush();

		} catch (IOException e) {
			log("ERROR", "Lỗi khi gửi file: " + e.getMessage());
		}
	}

	private void sendFileToClient(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			sendMessage("FILE_NOT_FOUND", "File không tồn tại.");
			log("FILE_NOT_FOUND", "File không tồn tại: " + filePath);
			return;
		}

		JsonObject fileInfoJson = new JsonObject();
		fileInfoJson.addProperty("fileName", file.getName());
		fileInfoJson.addProperty("fileSize", file.length());
		sendMessage("RES-FILE", fileInfoJson);
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			long totalBytesSent = 0;

			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				dataOut.write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;
			}
			dataOut.flush();

		} catch (IOException e) {
			log("ERROR", "Lỗi khi gửi file: " + e.getMessage());
		}
	}

	private void loadImageFileToClient(String filePath, String mes) {
		File file = new File(filePath);
		if (!file.exists()) {
			log("FILE_NOT_FOUND", "File không tồn tại: " + filePath);
			return;
		}

		JsonObject fileInfoJson = new JsonObject();
		fileInfoJson.addProperty("fileName", file.getName());
		fileInfoJson.addProperty("filePath", filePath);
		fileInfoJson.addProperty("fileSize", file.length());
		sendMessage("MESSAGE-FILE", fileInfoJson);
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			long totalBytesSent = 0;

			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				dataOut.write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;
			}
			dataOut.flush();

		} catch (IOException e) {
			sendMessage("FILE_TRANSFER_ERROR", "Lỗi trong quá trình gửi file.");
			log("ERROR", "Lỗi khi gửi file: " + e.getMessage());
		}
	}

	private void loadFileToClientAsyn(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			log("FILE_NOT_FOUND", "File không tồn tại: " + filePath);
			return;
		}

		JsonObject fileInfoJson = new JsonObject();
		fileInfoJson.addProperty("fileName", file.getName());
		fileInfoJson.addProperty("filePath", filePath);
		fileInfoJson.addProperty("fileSize", file.length());
		sendMessage("MESSAGE-ASYN-FILE", fileInfoJson);
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			long totalBytesSent = 0;

			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				dataOut.write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;
			}
			dataOut.flush();

		} catch (IOException e) {
			sendMessage("FILE_TRANSFER_ERROR", "Lỗi trong quá trình gửi file.");
			log("ERROR", "Lỗi khi gửi file: " + e.getMessage());
		}
	}

	private void notifyUserIsOnline(String username) {
		if (!isOnline(username)) {
			broadcastMessage("USER_DISCONNECTED", username);
			log("USER_DISCONNECTED", "User " + username + " đã ngắt kết nối hoàn toàn.");
		} else {
			broadcastMessage("USER_CONNECTED", username);
			log("USER_CONNECTED", "User " + username + " đã kết nối đến SERVER.");
		}
	}

	public void handleGetGroupTwoUser(SendMessageRequest request) {
		User ent01 = request.getUserSender();
		User ent02 = request.getUserReceive();

		int id01 = ent01.getId();
		int id02 = ent02.getId();

		String code = request.getCode();
		Conversation con = dbConnect.getConversationByCode(code);
		if (con == null) {
			con = dbConnect.addConversation(code, code);
			dbConnect.addParticipant(con.getId(), id01, 0);
			dbConnect.addParticipant(con.getId(), id02, 0);
		}

		File file = new File(ent02.getInfo03());
		if (!file.exists()) {
			sendMessage("FILE_NOT_FOUND", "File không tồn tại.");
			log("FILE_NOT_FOUND", "File không tồn tại: " + ent02.getInfo03());
			return;
		}

		JsonObject fileInfoJson = new JsonObject();
		fileInfoJson.addProperty("fileName", file.getName());
		fileInfoJson.addProperty("fileSize", file.length());
		sendMessage("AVATAR-GROUP", fileInfoJson);
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[4096];
			int bytesRead;
			long totalBytesSent = 0;

			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				dataOut.write(buffer, 0, bytesRead);
				totalBytesSent += bytesRead;
			}
			dataOut.flush();

		} catch (IOException e) {
			log("ERROR", "Lỗi khi gửi file: " + e.getMessage());
		}

		List<Message> lstMess = dbConnect.getLstMessage(con.getId());
		request.setConversation(con);
		sendMessage("RESGROUP2USER", request);
		sendMessage("LST-MESSAGE", lstMess);

	}

	public String imageToBase64(String filePath) {
		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("File không tồn tại: " + filePath);
			return null;
		}

		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] imageBytes = new byte[(int) file.length()];
			fileInputStream.read(imageBytes);
			return Base64.getEncoder().encodeToString(imageBytes);
		} catch (IOException e) {
			System.out.println("Lỗi khi đọc file: " + e.getMessage());
			return null;
		}
	}

	private void log(String code, String msg) {
		String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		String logEntry = String.format("[%s] CODE: %s, MESSAGE: %s", timestamp, code, msg);
		System.out.println(logEntry);
	}

	private void handleFileTransfer(SendMessageRequest request) {
		try {
			String fileInfoJson = dataIn.readUTF();
			JsonObject fileInfo = JsonParser.parseString(fileInfoJson).getAsJsonObject();
			String fileName = fileInfo.get("fileName").getAsString();
			long fileSize = fileInfo.get("fileSize").getAsLong();
			String serverFilePath = "files\\" + fileName;

			try (FileOutputStream fileOutputStream = new FileOutputStream(serverFilePath)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				long totalBytesReceived = 0;

				while (totalBytesReceived < fileSize) {
					bytesRead = dataIn.read(buffer);
					if (bytesRead == -1)
						break;
					fileOutputStream.write(buffer, 0, bytesRead);
					totalBytesReceived += bytesRead;

					log("FILE_PROGRESS",
							"Đã nhận " + bytesRead + " byte(s), tổng cộng: " + totalBytesReceived + " byte(s).");
				}
				if (isImageFile(fileName)) {

					dbConnect.insertMessage(request.getConversation().getId(), request.getUserSender().getId(), 2,
							request.getMessageText(), serverFilePath);
				} else {
					dbConnect.insertMessage(request.getConversation().getId(), request.getUserSender().getId(), 3,
							request.getMessageText(), serverFilePath);
				}
				if (isOnline(request.getUserReceive().getUsername())) {
					List<ServerHandler> senderHandlers = usernameToHandlers.get(request.getUserReceive().getUsername());

					for (ServerHandler handler : senderHandlers) {
						if (handler != this) {
							handler.sendFileToClient(serverFilePath, request.getMessageText());
						}
					}
				}

			} catch (IOException e) {
				sendMessage("FILE_TRANSFER_ERROR", "Lỗi trong quá trình nhận file: " + e.getMessage());
				log("ERROR", "Lỗi khi nhận file: " + e.getMessage());
			}
		} catch (IOException e) {
			sendMessage("FILE_TRANSFER_ERROR", "Lỗi trong quá trình đọc thông tin file: " + e.getMessage());
			log("ERROR", "Lỗi khi đọc thông tin file: " + e.getMessage());
		}
	}

	private boolean isImageFile(String fileName) {
		String lowerCaseFileName = fileName.toLowerCase();
		return lowerCaseFileName.endsWith(".jpg") || lowerCaseFileName.endsWith(".jpeg")
				|| lowerCaseFileName.endsWith(".png") || lowerCaseFileName.endsWith(".gif")
				|| lowerCaseFileName.endsWith(".bmp") || lowerCaseFileName.endsWith(".webp")
				|| lowerCaseFileName.endsWith(".svg");
	}

}
