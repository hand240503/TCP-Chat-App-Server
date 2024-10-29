package com.ndh.model;

public class User {
    private int id;           // ID của người dùng
    private String username;  // Tên đăng nhập
    private String password;  // Mật khẩu (nên mã hóa khi lưu trữ)
    private String avatar;     // Email của người dùng (nếu cần)

    // Constructor
    public User(int id, String username, String password, String avatar) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.avatar = avatar;
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

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}


}
