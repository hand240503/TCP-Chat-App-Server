package com.ndh.request;


public class Request {
    private String code;  // Mã yêu cầu
    private Object data;   // Dữ liệu gửi đi

    public Request() {
        // Constructor mặc định
    }

    public Request(String code, Object data) {
        this.code = code;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
