package com.example.womensafety_project;


public class ChatModel {
    private String message;
    private boolean isUser; // true if User, false if Bot

    public ChatModel(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }
}