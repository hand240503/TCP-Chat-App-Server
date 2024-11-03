package com.ndh.model;

import java.sql.Timestamp;
import java.util.List;

public class Conversation {

	private int id;
	private String title;
	private String code;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private List<Message> lstMes;

	public Conversation() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Conversation(int id, String title, String code, Timestamp createdAt, Timestamp updatedAt) {
		super();
		this.id = id;
		this.title = title;
		this.code = code;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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

	public List<Message> getLstMes() {
		return lstMes;
	}

	public void setLstMes(List<Message> lstMes) {
		this.lstMes = lstMes;
	}

}
