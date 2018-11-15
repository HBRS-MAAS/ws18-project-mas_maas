package org.mas_maas.messages;

import java.util.Vector;

public class ProofingRequest extends RequestMessage {
    private Float proofingTime;

    public ProofingRequest(String productType, Vector<String> guids, Float proofingTime) {
        super(productType, guids);
        this.proofingTime = proofingTime;
    }

    public Float getProofingTime() {
        return proofingTime;
    }

    public void setProofingTime(Float proofingTime) {
        this.proofingTime = proofingTime;
    }

    @Override
    public String toString() {
        return "ProofingRequest [proofingTime=" + proofingTime + "]";
    }
}
