package vn.neosoft;

public class User {
    private String username;
    private String password;
    private String name;
    private String role;
    private String phone;
    private String status;
    private int points;

    public User(String username, String password, String name, String role, String phone, int points) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.points = points;
    }
    public User(String username, String password, String name, String role, String phone, String status, int points) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.status = status;
        this.points = points;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
