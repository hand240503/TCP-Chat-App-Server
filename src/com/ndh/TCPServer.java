package com.ndh;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class TCPServer {

	private static List<ServerHandler> clientHandlers = new ArrayList<>();
	private static final int MAX_CLIENTS = 100;
	private static ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);
	private static DBConnect dbConnect;
	private static final int PORT = 12345;

	public static void main(String[] args) {


		dbConnect = new DBConnect();

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Server đang lắng nghe trên cổng " + PORT);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Kết nối từ: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

				ServerHandler clientHandler = new ServerHandler(clientSocket, clientHandlers, dbConnect);
				clientHandlers.add(clientHandler);
				executorService.execute(clientHandler);
			}
		} catch (IOException e) {
			System.out.println("Lỗi: " + e.getMessage());
		} finally {
			executorService.shutdown();
			if (dbConnect != null) {
				dbConnect.closeConnection();
			}
		}
	}
}
