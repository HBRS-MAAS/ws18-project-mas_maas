package org.mas_maas.objects;

public class Step
{
    private String action;
    private Float duration;
    public final static String KNEADING_TIME = "kneadingTime";
    public final static String PROOFING_TIME = "proofingTime";
    public final static String KNEADING_STEP = "kneadingStep";
    public final static String COOLING_STEP =  "coolingStep";
    public final static String PROOFING_STEP = "proofingStep";

    public Step(String action, Float duration2) {
        this.action = action;
        this.duration = duration2;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Float getDuration() {
        return duration;
    }

    public void setDuration(Float duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Step [action=" + action + ", duration=" + duration + "]";
    }
}
