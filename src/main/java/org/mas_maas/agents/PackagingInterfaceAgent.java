package org.mas_maas.agents;
import java.util.concurrent.atomic.AtomicBoolean;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.maas.agents.BaseAgent;

public class PackagingInterfaceAgent extends BaseAgent {
    private AID [] coolingRackAgents;

    private AtomicBoolean processingMessage = new AtomicBoolean(false);

    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("Packaging-interface", "JADE-bakery");
        this.getCoolingRackAIDs();

        addBehaviour(new ReceiveLoadBayNotifications());

    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }

            // we're only done when all messages are processed this time step
            if (!processingMessage.get()) {
                baseAgent.finished();
            }
        }
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
        processingMessage.set(true);

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
            processingMessage.set(false);
        }
        else {
            processingMessage.set(false);
            block();
        }
    }
}


}
