<<<<<<< HEAD
package org.maas.objects;
=======
package org.maas.Objects;
>>>>>>> 298926414bfbfeb7024e795c3e59e1eeaeaaa5f9

abstract public class Equipment {
    private String guid;

    public Equipment(String guid) {
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
        return "Equipment [guid=" + guid + "]";
    }
}
