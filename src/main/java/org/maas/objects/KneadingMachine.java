<<<<<<< HEAD
package org.maas.objects;
=======
package org.maas.Objects;
>>>>>>> 298926414bfbfeb7024e795c3e59e1eeaeaaa5f9

public class KneadingMachine extends Equipment {

    public KneadingMachine(String guid) {
        super(guid);
    }

    @Override
    public String toString() {
        return "KneadingMachine [guid=" + getGuid() + "]";
    }

}
