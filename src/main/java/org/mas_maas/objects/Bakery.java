package org.mas_maas.objects;

import java.awt.geom.Point2D;

public class Bakery {
        private String guid;
        private String name;
        private Point2D location;

        public Bakery(String guid, String name, Double x, Double y)
        {
            this.guid = guid;
            this.name = name;
            this.location = new Point2D.Double(x, y);
        }

        public Bakery(String guid, String name, Point2D location)
        {
            this.guid = guid;
            this.name = name;
            this.location = location;
        }

        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Bakery [guid=" + guid + ", name=" + name + ", location=" + location + "]";
        }
}