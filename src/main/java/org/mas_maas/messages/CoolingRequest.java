package org.mas_maas.messages;

public class CoolingRequest {
    private String productName;
    private float coolingTime;
    private int quantity;
    private int boxingTemp;

    public CoolingRequest(String productName, float coolingTime, int quantity, int boxingTemp) {
        super();
        this.productName = productName;
        this.coolingTime = coolingTime;
        this.quantity = quantity;
        this.boxingTemp = boxingTemp;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public float getCoolingTime() {
        return coolingTime;
    }

    public void setCoolinTime(int coolingTime) {
        this.coolingTime = coolingTime;
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
        return "CoolingRequest [productName=" + productName + ", coolingTime=" + coolingTime + ", quantity=" + quantity
                + ", boxingTemp=" + boxingTemp + "]";
    }
}
