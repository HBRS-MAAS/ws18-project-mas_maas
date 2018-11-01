package org.mas_maas;

import java.util.List;
import java.util.Vector;
import org.mas_maas.agents.Customer;
import org.mas_maas.agents.OrderProcessing;

public class Start {
    public static void main(String[] args) {

    	List<String> agents = new Vector<>();
    	agents.add("Customer:org.mas_maas.agents.Customer");
        agents.add("OrderProcessing:org.mas_maas.agents.OrderProcessing");

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
