package com.example.womensafety_project;

public class ContactModel {
    private String name;
    private String phone;
    private String key;

    // Inga dhaan bracket irundhudhu, adhai remove panni kela kondu vaanga
    public ContactModel() {
    }

    public ContactModel(String name, String phone, String key) {
        this.name = name;
        this.phone = phone;
        this.key = key;
    }

    // Getters and Setters logic (All inside this class)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
} // Indha bracket kadaisiyil thaan varanum