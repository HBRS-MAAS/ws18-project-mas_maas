package org.maas;

import java.util.Vector;

public class DoughStageVisualization extends Initializer {
    private String endTime;

    public DoughStageVisualization(String endTime) {
        this.endTime = endTime;
    }

    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();

        System.out.println("Trying to launch the logging Agent");
        agents.add("visualisation:org.mas_maas.agents.LoggingAgent(" + scenarioDirectory + ", " + endTime + ")");

        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
}
