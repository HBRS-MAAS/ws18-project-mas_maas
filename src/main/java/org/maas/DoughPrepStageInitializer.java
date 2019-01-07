package org.maas;

import java.util.Vector;
import org.maas.Initializer;
// import org.mas_maas.*;

public class DoughPrepStageInitializer extends Initializer {
    @Override
    public String initialize(String scenarioDirectory) {
        Vector<String> agents = new Vector<>();
        agents.add("AgentCreator:org.mas_maas.agents.AgentCreator");
        agents.add("DummyOrderProcesser:org.mas_maas.agents.DummyOrderProcesser");
        String agentInitString = String.join(";", agents);
        agentInitString += ";";
        return agentInitString;
    }
}
