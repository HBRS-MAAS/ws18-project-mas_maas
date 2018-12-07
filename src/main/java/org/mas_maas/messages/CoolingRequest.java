package org.mas_maas.messages;

public class CoolingRequest {
    private String productName;
    private float coolingTime;
    private int quantity;

    public CoolingRequest(String productName, float coolingTime, int quantity) {
        super();
        this.productName = productName;
        this.coolingTime = coolingTime;
        this.quantity = quantity;
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


    @Override
    public String toString() {
        return "CoolingRequest [productName=" + productName + ", coolingTime=" + coolingTime + ", "
        		+ "quantity=" + quantity + "]";
        		      		
    }
}
