<<<<<<< HEAD
package org.maas.objects;
=======
package org.maas.Objects;
>>>>>>> 298926414bfbfeb7024e795c3e59e1eeaeaaa5f9

public class Batch {
    private int breadsPerOven;

    public Batch(int breadsPerOven) {
        super();
        this.breadsPerOven = breadsPerOven;
    }

    private int getBreadsPerOven() {
        return breadsPerOven;
    }

    private void setBreadsPerOven(int breadsPerOven) {
        this.breadsPerOven = breadsPerOven;
    }

    @Override
    public String toString() {
        return "Batch [breadsPerOven=" + breadsPerOven + "]";
    }
}
