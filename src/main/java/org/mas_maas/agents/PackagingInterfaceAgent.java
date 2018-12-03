package org.mas_maas.agents;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PackagingInterfaceAgent extends BaseAgent {
    private AID [] coolingRackAgents;

    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("Packaging-interface", "JADE-bakery");
        this.getCoolingRackAIDs();

        addBehaviour(new ReceiveLoadBayNotifications());

    }

    public void getCoolingRackAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("CoolingRack");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Cooling agents:");
            coolingRackAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                coolingRackAgents[i] = result[i].getName();
                System.out.println(coolingRackAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    /* This is the behaviour used for receiving doughNotifications */
  private class ReceiveLoadBayNotifications extends CyclicBehaviour {
    public void action() {
        // baseAgent.finished(); //call it if there are no generic behaviours
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
            MessageTemplate.MatchConversationId("loadingBay-message"));

        ACLMessage msg = baseAgent.receive(mt);

        if (msg != null) {
            System.out.println("-------> " + getAID().getLocalName()+" Received cooling bay msg from " + msg.getSender());

            String content = msg.getContent();

            ACLMessage reply = msg.createReply();

            reply.setPerformative(ACLMessage.CONFIRM);

            reply.setContent("Cooling Bay msg was received");

            baseAgent.sendMessage(reply);

            System.out.println(content);

            myAgent.doDelete();

        }
        else {
            block();
        }
    }
}


}
