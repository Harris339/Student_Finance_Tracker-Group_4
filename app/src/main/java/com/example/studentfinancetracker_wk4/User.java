package com.example.studentfinancetracker_wk4;

public class User {

    private String fname;
    private String lname;
    private String username;
    private String password;

    public User(String fname, String lname, String username, String password){
        this.fname = fname;
        this.lname = lname;
        this.username = username;
        this.password = password;
    }

    public User(){
        fname = "";
        lname = "";
        username = "";
        password = "";

    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
