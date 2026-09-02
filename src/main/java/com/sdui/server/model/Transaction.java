package com.sdui.server.model;

public class Transaction {
    private String id;
    private String description;
    private String amount;
    private String type;
    private String date;

    public Transaction(String id, String description, String amount, String type, String date) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.date = date;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getAmount() { return amount; }
    public String getType() { return type; }
    public String getDate() { return date; }
}
