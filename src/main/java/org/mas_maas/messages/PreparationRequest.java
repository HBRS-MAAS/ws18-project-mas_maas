package org.mas_maas.messages;

import java.util.Vector;

import org.mas_maas.objects.Step;

public class PreparationRequest extends RequestMessage {
    private Vector<Integer> productQuantities;
    private Vector<Step> steps;

    public PreparationRequest(String productType, Vector<String> guids,
            Vector<Integer> productQuantities, Vector<Step> steps) {
        super(productType, guids);
        this.productQuantities = productQuantities;
        this.steps = steps;
    }

    public Vector<Integer> getProductQuantities() {
        return productQuantities;
    }

    public void setProductQuantities(Vector<Integer> productQuantities) {
        this.productQuantities = productQuantities;
    }

    public Vector<Step> getSteps() {
        return steps;
    }

    public void setSteps(Vector<Step> steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "PreparationRequest [productQuantities=" + productQuantities + "]";
    }
}
