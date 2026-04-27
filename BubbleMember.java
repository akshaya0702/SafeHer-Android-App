package com.example.womensafety_project;

public class BubbleMember {
    public String name, phone, imageUrl;
    public double latitude, longitude;

    public BubbleMember() {} // Firebase-ku idhu kattayam venum

    public BubbleMember(String name, String phone, double latitude, double longitude) {
        this.name = name;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
