package com.example.entity;

/**
 * 人员表邮箱。邮件显示名只用姓名；代码里用账号 + 角色 + 邮箱识别和去重。
 */
public class PersonMailAddress {
    private String username;
    private String name;
    private String email;
    private String role;

    public PersonMailAddress() {
    }

    public PersonMailAddress(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public PersonMailAddress(String username, String name, String email) {
        this(username, name, email, null);
    }

    public PersonMailAddress(String username, String name, String email, String role) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** 邮件个人名：只要姓名。 */
    public String personal() {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return email;
    }

    public String display() {
        String shown = personal();
        if (email == null || email.trim().isEmpty()) {
            return shown;
        }
        return shown + " <" + email + ">";
    }
}
