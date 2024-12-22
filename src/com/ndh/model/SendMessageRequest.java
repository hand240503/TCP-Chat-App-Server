package com.ndh.model;

public class SendMessageRequest {
	private User userSender;
	private User userReceive;
	private String messageText;
	private String code;
	private Conversation conversation;
	private int portSender;
	private int portReceiver;
	private String ip;
	public SendMessageRequest() {
	}

	public SendMessageRequest(User userSender, User userReceive, String messageText, String code,
			Conversation conversation) {
		this.userSender = userSender;
		this.userReceive = userReceive;
		this.messageText = messageText;
		this.code = code;
		this.conversation = conversation;
	}

	public SendMessageRequest(User userSender, User userReceive, String messageText, String code,
			Conversation conversation, int portSender, int portReceiver,String ip) {
		this.userSender = userSender;
		this.userReceive = userReceive;
		this.messageText = messageText;
		this.code = code;
		this.conversation = conversation;
		this.portSender = portSender;
		this.portReceiver = portReceiver;
		this.ip = ip;
	}

	public User getUserSender() {
		return userSender;
	}

	public void setUserSender(User userSender) {
		this.userSender = userSender;
	}

	public User getUserReceive() {
		return userReceive;
	}

	public void setUserReceive(User userReceive) {
		this.userReceive = userReceive;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Conversation getConversation() {
		return conversation;
	}

	public void setConversation(Conversation conversation) {
		this.conversation = conversation;
	}

	public int getPortSender() {
		return portSender;
	}

	public void setPortSender(int portSender) {
		this.portSender = portSender;
	}

	public int getPortReceiver() {
		return portReceiver;
	}

	public void setPortReceiver(int portReceiver) {
		this.portReceiver = portReceiver;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	
}
