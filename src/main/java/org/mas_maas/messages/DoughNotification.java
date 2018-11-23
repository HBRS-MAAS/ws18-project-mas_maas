package org.mas_maas.messages;

import java.util.Vector;

public class DoughNotification extends GenericGuidMessage {
    Vector<Integer> productQuantities;

    public DoughNotification(Vector<String> guids, String productType, Vector<Integer> productQuantities) {
        super(guids, productType);
        this.productQuantities = productQuantities;
    }
}
