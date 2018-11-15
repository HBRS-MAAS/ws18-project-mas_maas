package org.mas_maas.agents;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Proofer extends BaseAgent {
    private AID [] doughManagerAgents;
    private AID [] bakingInterfaceAgents;
    
    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("Proofer", "JADE-bakery");
        
    }
        
    public void getDoughManagerAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Dough-manager");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Dough-manager agents:");
            doughManagerAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                doughManagerAgents[i] = result[i].getName();
                System.out.println(doughManagerAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }    
    }
    
    public void getBakingInterfaceAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Baking-interface");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Baking-interface agents:");
            bakingInterfaceAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                bakingInterfaceAgents[i] = result[i].getName();
                System.out.println(bakingInterfaceAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }    
    }
    
    


}
