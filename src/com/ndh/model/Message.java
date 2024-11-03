package com.ndh.model;

import java.sql.Timestamp;

public class Message {

	private int id;
	private int conversationId;
	private int senderId;
	private int type;
	private String message;
	private String info01;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private String senderUser;

	public Message() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Message(int id, int conversationId, int senderId, int type, String message, String info01,
			Timestamp createdAt, Timestamp updatedAt) {
		super();
		this.id = id;
		this.conversationId = conversationId;
		this.senderId = senderId;
		this.type = type;
		this.message = message;
		this.info01 = info01;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public String getInfo01() {
		return info01;
	}

	public void setInfo01(String info01) {
		this.info01 = info01;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getConversationId() {
		return conversationId;
	}

	public void setConversationId(int conversationId) {
		this.conversationId = conversationId;
	}

	public int getSenderId() {
		return senderId;
	}

	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getSenderUser() {
		return senderUser;
	}

	public void setSenderUser(String senderUser) {
		this.senderUser = senderUser;
	}
	
	

}
