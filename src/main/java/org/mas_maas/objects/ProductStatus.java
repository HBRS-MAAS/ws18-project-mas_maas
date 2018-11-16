package org.mas_maas.objects;

public class ProductStatus {
    private String guid;
    private String status;
    private int amount;
    private String productType;

    public ProductStatus(String guid, String status, int amount, String productType) {
        this.guid = guid;
        this.status = status;
        this.amount = amount;
        this.productType = productType;
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

    public String getProductType() {
        return productType;
    }
}
