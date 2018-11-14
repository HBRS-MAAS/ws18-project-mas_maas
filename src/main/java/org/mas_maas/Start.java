package org.mas_maas;

import java.util.List;
import java.util.Vector;

public class Start {
    public static void main(String[] args) {
        JSONConverter.test_parsing();

        List<String> agents = new Vector<>();
        agents.add("Customer:org.mas_maas.agents.Customer");
        agents.add("OrderProcessing:org.mas_maas.agents.OrderProcessing");
        agents.add("Scheduler:org.mas_maas.agents.Scheduler");
        agents.add("DoughMaker:org.mas_maas.agents.DoughMaker");
        agents.add("OvenManager:org.mas_maas.agents.OvenManager");
        agents.add("DeliveryManager:org.mas_maas.agents.DeliveryManager");

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
