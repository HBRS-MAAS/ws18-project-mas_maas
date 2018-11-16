package org.mas_maas.messages;

import java.util.Vector;

public class DoughNotification extends DoughMessage {
    private int quantity;

    public DoughNotification(Vector<String> guids, String productType, int quantity) {
        super(guids, productType);
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "DoughNotification [quantity=" + quantity + "]";
    }
}
