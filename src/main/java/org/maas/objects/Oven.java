<<<<<<< HEAD
package org.maas.objects;
=======
package org.maas.Objects;
>>>>>>> 298926414bfbfeb7024e795c3e59e1eeaeaaa5f9

public class Oven extends Equipment {
    private int coolingRate;
    private int heatingRate;

    public Oven(String guid, int coolingRate, int heatingRate) {
        super(guid);
        this.coolingRate = coolingRate;
        this.heatingRate = heatingRate;
    }

    public int getCoolingRate() {
        return coolingRate;
    }

    public void setCoolingRate(int coolingRate) {
        this.coolingRate = coolingRate;
    }

    public int getHeatingRate() {
        return heatingRate;
    }

    public void setHeatingRate(int heatingRate) {
        this.heatingRate = heatingRate;
    }

    @Override
    public String toString() {
        return "Oven [" + "guid=" + this.getGuid() + ", coolingRate=" + coolingRate + ", heatingRate=" + heatingRate + "]";
    }

}
