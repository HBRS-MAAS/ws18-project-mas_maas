package org.mas_maas.messages;

import java.util.Vector;

public class DoughNotification extends GenericGuidMessage {
    public DoughNotification(Vector<String> guids, String productType) {
        super(guids, productType);
    }
}
