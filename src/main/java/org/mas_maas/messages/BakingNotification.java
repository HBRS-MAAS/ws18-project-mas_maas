package org.mas_maas.messages;

import java.util.Vector;

public class BakingNotification extends GenericGuidMessage {

    public BakingNotification(Vector<String> guids, String productType) {
        super(guids, productType);
    }
}
