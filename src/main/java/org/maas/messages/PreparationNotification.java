package org.maas.messages;

import java.util.Vector;

public class PreparationNotification extends GenericGuidMessage {
    private int quantity;
    public PreparationNotification(Vector<String> guids, String productType, int quantity) {
        super(guids, productType);
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

}
