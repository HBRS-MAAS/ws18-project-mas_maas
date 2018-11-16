package org.mas_maas.objects;

public class OrderStatus {
    private String guid;
    private String status;
    private int amount;
    private String type;

    public OrderStatus(String guid, String status, int amount, String type) {
        this.guid = guid;
        this.status = status;
        this.amount = amount;
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getGuid() {
        return guid;
    }

    public int getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }
}
