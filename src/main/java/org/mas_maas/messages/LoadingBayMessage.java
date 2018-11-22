package org.mas_maas.messages;

import java.util.Vector;

public class LoadingBayMessage {

    public Vector<ProductPair> products;

    public LoadingBayMessage()
    {
        this.products = new Vector<ProductPair>();
    }

    public void addProduct(String productName, int quantity)
    {
        this.products.add(new ProductPair(productName, quantity));
    }


    private class ProductPair {
        private String productName;
        private int quantity;

        private ProductPair(String productName, int quantity)
        {
            this.productName = productName;
            this.quantity = quantity;
        }
    }
}
