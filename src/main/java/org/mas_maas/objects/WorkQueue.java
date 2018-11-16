package org.mas_maas.objects;

import java.util.LinkedList;
import java.util.Vector;

public class WorkQueue {
    private LinkedList<ProductStatus> workQueue;

    public WorkQueue()
    {
         this.workQueue = new LinkedList<ProductStatus>();
    }

    public void addProduct(ProductStatus productStatus)
    {
        this.workQueue.add(productStatus);
    }

    public void addProducts(Iterable<ProductStatus> products)
    {
        for (ProductStatus product : products)
        {
            this.addProduct(product);
        }
    }

    public ProductStatus getFirstProduct()
    {
        ProductStatus firstProduct = null;

        if (!this.workQueue.isEmpty())
        {
            firstProduct = this.workQueue.pop();
        }

        return firstProduct;
    }

    // find all items of the same type as the first item
    // return all the items of the same stage or null if workQueue is empty
    public Vector<ProductStatus> getProductBatch()
    {
        ProductStatus firstProduct = this.getFirstProduct();
        Vector<ProductStatus> batch = null;

        if (firstProduct != null)
        {
            batch = new Vector<ProductStatus>();
            String curStatus = firstProduct.getStatus();

            for (ProductStatus productStatus : this.workQueue)
            {
                if (productStatus.getStatus().equals(curStatus))
                {
                    batch.add(productStatus);
                    this.workQueue.remove(productStatus);
                }
            }
        }

        return batch;
    }

}
