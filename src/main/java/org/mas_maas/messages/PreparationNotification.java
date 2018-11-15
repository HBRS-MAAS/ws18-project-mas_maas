package org.mas_maas.messages;

public class PreparationNotification extends NotificationMessage {
    private String productType;

    public PreparationNotification(String guid, String productType) {
        super(guid);
        this.productType = productType;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    @Override
    public String toString() {
        return "PreparationNotification [productType=" + productType + "]";
    }
}
