package org.mas_maas.messages;

import java.util.Vector;
import org.mas_maas.objects.CoolingRequestTuple;

public class CoolingRequest {
    public Vector<CoolingRequestTuple> coolingRequests;

    public CoolingRequest()
    {
        this.coolingRequests = new Vector<CoolingRequestTuple>();
    }

    public void addCoolingRequest(String guid, float coolingDuration, int quantity)
    {
        this.coolingRequests.add(new CoolingRequestTuple(guid, coolingDuration, quantity));
    }

    @Override
    public String toString() {
        return "CoolingRequest [coolingRequests=" + coolingRequests + "]";
    }

   
}
