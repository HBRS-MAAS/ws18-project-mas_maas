package org.mas_maas.messages;

public abstract class NotificationMessage {

    private String guid;

    public NotificationMessage(String guid) {
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Override
    public String toString() {
        return "NotificationMessage [guid=" + guid + "]";
    }
}
