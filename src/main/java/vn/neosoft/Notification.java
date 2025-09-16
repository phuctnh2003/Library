package vn.neosoft;

import java.sql.Timestamp;

public class Notification {

    private int id;
    private String message;
    private String username;
    private boolean isRead;
    private Timestamp createdAt;

    // Constructor để khởi tạo đối tượng Notification
    public Notification(int id, String message, String username, boolean isRead, Timestamp createdAt) {
        this.id = id;
        this.message = message;
        this.username = username;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // Getters và Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }


}

