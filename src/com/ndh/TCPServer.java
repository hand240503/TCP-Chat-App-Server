package com.ndh;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {

	private static List<ServerHandler> clientHandlers = new ArrayList<>();
	private static Map<String, List<ServerHandler>> usernameToHandlers = new HashMap<>();
	private static final int MAX_CLIENTS = 100;
	private static ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);
	private static DBConnect dbConnect;
	private static final int PORT = 12345;

	public static void main(String[] args) {

		dbConnect = new DBConnect();

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			log("SERVER_STARTED", "Server đang lắng nghe trên cổng " + PORT);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				log("CLIENT_CONNECTED", "Kết nối từ: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

				ServerHandler clientHandler = new ServerHandler(clientSocket, clientHandlers, usernameToHandlers,
						dbConnect);
				clientHandlers.add(clientHandler);
				executorService.execute(clientHandler);
			}
		} catch (IOException e) {
			log("ERROR", "Lỗi: " + e.getMessage());
		} finally {
			executorService.shutdown();
			if (dbConnect != null) {
				dbConnect.closeConnection();
			}
		}
	}

	private static void log(String code, String msg) {
		String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		String logEntry = String.format("[%s] CODE: %s, MESSAGE: %s", timestamp, code, msg);
		System.out.println(logEntry);
	}
}
