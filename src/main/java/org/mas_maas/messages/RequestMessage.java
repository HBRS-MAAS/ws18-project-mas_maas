package org.mas_maas.messages;

import java.util.Vector;

public abstract class RequestMessage {

    private String productType;
    private Vector<String> guids;



    public RequestMessage(String productType, Vector<String> guids) {
        this.productType = productType;
        this.guids = guids;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Vector<String> getGuids() {
        return guids;
    }

    public void setGuids(Vector<String> guids) {
        this.guids = guids;
    }

    @Override
    public String toString() {
        return "RequestMessage [productType=" + productType + ", guids=" + guids + "]";
    }
}
