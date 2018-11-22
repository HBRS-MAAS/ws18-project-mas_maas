package org.mas_maas.messages;

public class CoolingRequest {
    private String productName;
    private int coolinRate;
    private int quantity;
    private int boxingTemp;

    public CoolingRequest(String productName, int coolinRate, int quantity, int boxingTemp) {
        super();
        this.productName = productName;
        this.coolinRate = coolinRate;
        this.quantity = quantity;
        this.boxingTemp = boxingTemp;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getCoolinRate() {
        return coolinRate;
    }

    public void setCoolinRate(int coolinRate) {
        this.coolinRate = coolinRate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getBoxingTemp() {
        return boxingTemp;
    }

    public void setBoxingTemp(int boxingTemp) {
        this.boxingTemp = boxingTemp;
    }

    @Override
    public String toString() {
        return "CoolingRequest [productName=" + productName + ", coolinRate=" + coolinRate + ", quantity=" + quantity
                + ", boxingTemp=" + boxingTemp + "]";
    }
}
