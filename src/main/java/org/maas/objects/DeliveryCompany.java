<<<<<<< HEAD
package org.maas.objects;
=======
package org.maas.Objects;
>>>>>>> 298926414bfbfeb7024e795c3e59e1eeaeaaa5f9

import java.awt.geom.Point2D;
import java.util.Vector;

public class DeliveryCompany {
    private String guid;
    private Point2D location;
    private Vector<Truck> trucks;

    public DeliveryCompany(String guid, Point2D location, Vector<Truck> trucks) {
        super();
        this.guid = guid;
        this.location = location;
        this.trucks = trucks;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Point2D getLocation() {
        return location;
    }

    public void setLocation(Point2D location) {
        this.location = location;
    }

    public Vector<Truck> getTrucks() {
        return trucks;
    }

    public void setTrucks(Vector<Truck> trucks) {
        this.trucks = trucks;
    }

    @Override
    public String toString() {
        return "DeliveryCompany [guid=" + guid + ", location=" + location + ", trucks=" + trucks + "]";
    }
}
