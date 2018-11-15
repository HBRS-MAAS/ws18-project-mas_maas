package org.mas_maas.messages;

import java.util.Vector;

public class KneadingRequest extends RequestMessage {
    private Float kneadingTime;

    public KneadingRequest(String productType, Vector<String> guids, Float kneadingTime) {
        super(productType, guids);
        this.kneadingTime = kneadingTime;
    }

    public Float getKneadingTime() {
        return kneadingTime;
    }

    public void setKneadingTime(Float kneadingTime) {
        this.kneadingTime = kneadingTime;
    }

    @Override
    public String toString() {
        return "KneadingRequest [kneadingTime=" + kneadingTime + "]";
    }
}
