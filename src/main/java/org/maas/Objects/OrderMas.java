package org.maas.Objects;

import java.util.Vector;

public class OrderMas {

    private String customer_id;
    private String guid;

    public class OrderDate
    {
        private int order_day;
        private int order_hour;
        public OrderDate(int order_day, int order_hour) {
            super();
            this.order_day = order_day;
            this.order_hour = order_hour;
        }
        public int getOrder_day() {
            return order_day;
        }
        public void setOrder_day(int order_day) {
            this.order_day = order_day;
        }
        public int getOrder_hour() {
            return order_hour;
        }
        public void setOrder_hour(int order_hour) {
            this.order_hour = order_hour;
        }
        @Override
        public String toString() {
            return "OrderDate [order_day=" + order_day + ", order_hour=" + order_hour + "]";
        }
    }

    public class DeliveryDate
    {
        private int delivery_day;
        private int delivery_hour;
        public DeliveryDate(int delivery_day, int delivery_hour) {
            super();
            this.delivery_day = delivery_day;
            this.delivery_hour = delivery_hour;
        }
        public int getDelivery_day() {
            return delivery_day;
        }
        public void setDelivery_day(int delivery_day) {
            this.delivery_day = delivery_day;
        }
        public int getDelivery_hour() {
            return delivery_hour;
        }
        public void setDelivery_hour(int delivery_hour) {
            this.delivery_hour = delivery_hour;
        }
        @Override
        public String toString() {
            return "DeliveryDate [delivery_day=" + delivery_day + ", delivery_hour=" + delivery_hour + "]";
        }
    }

    // I thought about using a standard java object like Calendar or Date
    // but this seemed a better fit given the json format and our limited scope
    private OrderDate order_date;
    private DeliveryDate delivery_date;
    private Vector<BakedGood> bakedGoods;

    public OrderMas() {}

    public OrderMas(String customer_id, String guid, int order_day, int order_hour, int delivery_day, int delivery_hour,
            Vector<BakedGood> bakedGoods) {
        super();
        this.customer_id = customer_id;
        this.guid = guid;
        this.order_date = new OrderDate(order_day, order_hour);
        this.delivery_date = new DeliveryDate(delivery_day, delivery_hour);
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
