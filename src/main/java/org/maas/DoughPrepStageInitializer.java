package org.maas;

import java.util.Vector;
import org.maas.Initializer;
import org.maas.JSONConverter;
import org.maas.Objects.Bakery;
import org.maas.Objects.DoughPrepTable;
import org.maas.Objects.Equipment;
import org.maas.Objects.Oven;

public class DoughPrepStageInitializer extends Initializer {
    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();
        //agents.add("AgentCreator:org.mas_maas.agents.AgentCreator(" + scenarioDirectory + ")");
        agents.add("DummyOrderProcesser:org.mas_maas.agents.DummyOrderProcesser(" + scenarioDirectory+ ")");
        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
}
