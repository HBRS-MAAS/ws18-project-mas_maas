package org.mas_maas.objects;

import java.util.Vector;

public class Recipe{
    private int coolingRate;
    private int bakingTemp;
    private Vector<Step> steps; //inner class Step defined at the end of the file

    public Recipe(int coolingRate, int bakingTemp, Vector<Step> steps) {
        this.coolingRate = coolingRate;
        this.bakingTemp = bakingTemp;
        this.steps = steps;
    }

    public int getCoolingRate() {
        return coolingRate;
    }

    public void setCoolingRate(int coolingRate) {
        this.coolingRate = coolingRate;
    }

    public int getBakingTemp() {
        return bakingTemp;
    }

    public void setBakingTemp(int bakingTemp) {
        this.bakingTemp = bakingTemp;
    }

    public Vector<Step> getSteps() {
        return steps;
    }

    public void setSteps(Vector<Step> steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "Recipe [coolingRate=" + coolingRate + ", bakingTemp=" + bakingTemp + ", steps=" + steps + "]";
    }
}