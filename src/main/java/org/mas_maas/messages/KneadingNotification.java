package org.mas_maas.messages;

import java.util.Vector;

public class KneadingNotification extends DoughMessage {

    public KneadingNotification(Vector<String> guids, String productType) {
        super(guids, productType);
    }
}
