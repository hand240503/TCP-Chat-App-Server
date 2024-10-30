package com.ndh.model;

public class User {
	private int id; // ID của người dùng
	private String username; // Tên đăng nhập
	private String password; // Mật khẩu (nên mã hóa khi lưu trữ)
	private String info01;
	private String info02;
	private String avatar; // Email của người dùng (nếu cần)
	private boolean isOnline;

	// Constructor
	public User(int id, String username, String password, String info01, String info02, String avatar) {
		super();
		this.id = id;
		this.username = username;
		this.password = password;
		this.info01 = info01;
		this.info02 = info02;
		this.avatar = avatar;
	}

	public User() {

	}

	// Getter và Setter cho ID
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	// Getter và Setter cho username
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	// Getter và Setter cho password
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getInfo01() {
		return info01;
	}

	public void setInfo01(String info01) {
		this.info01 = info01;
	}

	public String getInfo02() {
		return info02;
	}

	public void setInfo02(String info02) {
		this.info02 = info02;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public boolean isOnline() {
		return isOnline;
	}

	public void setOnline(boolean isOnline) {
		this.isOnline = isOnline;
	}

}
