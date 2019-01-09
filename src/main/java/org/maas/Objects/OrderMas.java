package org.maas.Objects;

import java.util.Vector;

public class OrderMas {

    private String customer_id;
    private String guid;

    // I thought about using a standard java object like Calendar or Date
    // but this seemed a better fit given the json format and our limited scope
    private int orderDay;
    private int orderHour;
    private int delivery_date;
    private int delivery_hour;
    private Vector<BakedGood> bakedGoods;

    public OrderMas() {}

    public OrderMas(String customer_id, String guid, int orderDay, int orderHour, int delivery_date, int delivery_hour,
            Vector<BakedGood> bakedGoods) {
        super();
        this.customer_id = customer_id;
        this.guid = guid;
        this.orderDay = orderDay;
        this.orderHour = orderHour;
        this.delivery_date = delivery_date;
        this.delivery_hour = delivery_hour;
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

    public int getOrderDay() {
        return orderDay;
    }

    public void setOrderDay(int orderDay) {
        this.orderDay = orderDay;
    }

    public int getOrderHour() {
        return orderHour;
    }

    public void setOrderHour(int orderHour) {
        this.orderHour = orderHour;
    }

    public int getDelivery_date() {
        return delivery_date;
    }

    public void setDelivery_date(int delivery_date) {
        this.delivery_date = delivery_date;
    }

    public int getDelivery_hour() {
        return delivery_hour;
    }

    public void setDelivery_hour(int delivery_hour) {
        this.delivery_hour = delivery_hour;
    }

    public Vector<BakedGood> getBakedGoods() {
        return bakedGoods;
    }

    public void setBakedGoods(Vector<BakedGood> bakedGoods) {
        this.bakedGoods = bakedGoods;
    }

    @Override
    public String toString() {
        // return "OrderMas [customer_id=" + customer_id + ", guid=" + guid + ", order_date=[" + "day=" + orderDay + ", hour="
        //         + orderHour + "], delivery_date=[" + "day=" + delivery_date + ", hour=" + delivery_hour + "], bakedGoods="
        //         + bakedGoods + "]";

        return "Order [customer_id=" + customer_id + ", guid=" + guid + ", orderDay=" + orderDay + ", orderHour="
                + orderHour + ", delivery_date=" + delivery_date + ", delivery_hour=" + delivery_hour + ", bakedGoods="
                + bakedGoods + "]";
    }
}
