package com.ndh.model;

public class SendMessageRequest {
	private User userSender;
	private User userReceive;
	private String messageText;
	private String code;
	private Conversation conversation;

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

}
