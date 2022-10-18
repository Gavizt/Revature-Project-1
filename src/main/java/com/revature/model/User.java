package com.revature.model;

import java.util.Objects;

/**
 * User class for Employees and Managers:
 *      Each user must register with a unique username and a password.
 *      Role is set to "Employee" by default.
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


    /*
    Objects are differed by their unique id.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return this.id == user.id;
    }

    /*
    Generate hashcode with object's id.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return "User {" +
                "id=" + this.id +
                ",\trole='" + this.role + '\'' +
                ",\tusername='" + this.username + '\'' +
                ",\tpassword='" + this.password + '\'' +
                '}';
    }
}
