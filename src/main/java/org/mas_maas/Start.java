package org.mas_maas;

import java.util.List;
import java.util.Vector;

public class Start {
    public static void main(String[] args) {
        JSONConverter.test_parsing();

        List<String> agents = new Vector<>();
        agents.add("TimeKeeper:org.mas_maas.agents.TimeKeeper");
        //agents.add("CoolingRackAgent:org.maas.agents.CoolingRackAgent");
        //agents.add("Intermediater:org.mas_maas.agents.Intermediater");

        agents.add("TimeKeeper:org.mas_maas.agents.TimeKeeper");
        agents.add("DoughManager:org.mas_maas.agents.DoughManager");
        agents.add("KneadingMachineAgent:org.mas_maas.agents.KneadingMachineAgent");
        agents.add("PreparationTableAgent:org.mas_maas.agents.PreparationTableAgent");
        agents.add("Proofer:org.mas_maas.agents.Proofer");

        agents.add("BakingManager:org.mas_maas.agents.BakingManager");
        agents.add("OvenAgent:org.mas_maas.agents.OvenAgent");
        agents.add("BakingPreparationAgent:org.mas_maas.agents.BakingPreparationAgent");
        agents.add("CoolingAgent:org.mas_maas.agents.CoolingAgent");

//agents.add("PackagingInterfaceAgent:org.mas_maas.agents.PackagingInterfaceAgent");
        List<String> cmd = new Vector<>();
        cmd.add("-agents");
        StringBuilder sb = new StringBuilder();
        for (String a : agents) {
            sb.append(a);
            sb.append(";");
        }
        cmd.add(sb.toString());
        jade.Boot.main(cmd.toArray(new String[cmd.size()]));
    }
}
