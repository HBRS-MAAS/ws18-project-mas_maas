package org.mas_maas.objects;

public class Step
{
    private String action;
    private int duration;

    public Step(String action, int duration) {
        this.action = action;
        this.duration = duration;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Step [action=" + action + ", duration=" + duration + "]";
    }
}
