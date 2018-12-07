package org.mas_maas.messages;

import java.util.Vector;

class CoolingRequest {
    public Vector<CoolingRequestTuple> coolingRequests;

    public CoolingRequest()
    {
        this.coolingRequests = new Vector<CoolingRequestTuple>();
    }

    public void addCoolingRequest(String guid, float coolingDuration, int quantity, int boxingTemp)
    {
        this.coolingRequests.add(new CoolingRequestTuple(guid, coolingDuration, quantity));
    }

    @Override
    public String toString() {
        return "CoolingRequest [coolingRequests=" + coolingRequests + "]";
    }

    private class CoolingRequestTuple {
        private String guid;
        private int quantity;
        private float coolingDuration;

        public CoolingRequestTuple(String guid, float coolingDuration, int quantity) {
            this.guid = guid;
            this.coolingDuration = coolingDuration;
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return "CoolingRequest [guid=" + guid + ", coolingDuration=" + coolingDuration + ", quantity=" + quantity + "]";
        }
    }
}
