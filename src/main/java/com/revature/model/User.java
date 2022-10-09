package com.revature.model;

import java.util.Objects;

/**
 * User class.
 *
 * @author johnainsworth
 */
public class User {
    private long id;
    private String role;
    private String username;
    private String password;


    public User() {
        super();
    }

    public User(long id, String role, String username, String password) {
        this.id = id;
        this.role = role;
        this.username = username;
        this.password = password;
    }


    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return this.id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + this.id +
                ", role='" + this.role + '\'' +
                ", username='" + this.username + '\'' +
                ", password='" + this.password + '\'' +
                '}';
    }
}
