package org.maas.Objects;

import java.util.Vector;

public class OrderMas {

    private String customer_id;
    private String guid;

    public class OrderDate
    {
        private int day;
        private int hour;
        public OrderDate(int day, int hour) {
            super();
            this.day = day;
            this.hour = hour;
        }
        public int getOrder_day() {
            return day;
        }
        public void setOrder_day(int day) {
            this.day = day;
        }
        public int getOrder_hour() {
            return hour;
        }
        public void setOrder_hour(int hour) {
            this.hour = hour;
        }
        @Override
        public String toString() {
            return "OrderDate [day=" + day + ", hour=" + hour + "]";
        }
    }

    public class DeliveryDate
    {
        private int day;
        private int hour;
        public DeliveryDate(int day, int hour) {
            super();
            this.day = day;
            this.hour = hour;
        }
        public int getDelivery_day() {
            return day;
        }
        public void setDelivery_day(int day) {
            this.day = day;
        }
        public int getDelivery_hour() {
            return hour;
        }
        public void setDelivery_hour(int hour) {
            this.hour = hour;
        }
        @Override
        public String toString() {
            return "DeliveryDate [day=" + day + ", hour=" + hour + "]";
        }
    }

    // I thought about using a standard java object like Calendar or Date
    // but this seemed a better fit given the json format and our limited scope
    private OrderDate order_date;
    private DeliveryDate delivery_date;
    private Vector<BakedGood> bakedGoods;

    public OrderMas() {}

    public OrderMas(String customer_id, String guid, int day, int hour, int day, int hour,
            Vector<BakedGood> bakedGoods) {
        super();
        this.customer_id = customer_id;
        this.guid = guid;
        this.order_date = new OrderDate(day, hour);
        this.delivery_date = new DeliveryDate(day, hour);
        this.bakedGoods = bakedGoods;
    }

    public String getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(String customer_id) {
        this.customer_id = customer_id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public Vector<BakedGood> getBakedGoods() {
        return bakedGoods;
    }

    public void setBakedGoods(Vector<BakedGood> bakedGoods) {
        this.bakedGoods = bakedGoods;
    }

    @Override
    public String toString() {
        return "OrderMas [customer_id=" + customer_id + ", guid=" + guid + ", orderDate=" + order_date
                + ", deliveryDate=" + delivery_date + ", bakedGoods=" + bakedGoods + "]";
    }
}
