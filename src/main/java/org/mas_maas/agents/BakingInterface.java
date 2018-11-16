package org.mas_maas.agents;
import java.util.*;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BakingInterface extends BaseAgent {
	private AID [] prooferAgents;

	protected void setup() {
		super.setup();
		System.out.println(getAID().getLocalName() + " is ready.");
		this.register("Baking-interface", "JADE-bakery");
		this.getProoferAIDs();

		addBehaviour(new ReceiveDoughNotifications());

	}

	public void getProoferAIDs() {
		DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Proofer");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Proofer agents:");
            prooferAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                prooferAgents[i] = result[i].getName();
                System.out.println(prooferAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
	}

	/* This is the behaviour used for receiving doughNotifications */
  private class ReceiveDoughNotifications extends CyclicBehaviour {
	public void action() {
		// baseAgent.finished(); //call it if there are no generic behaviours
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		ACLMessage msg = baseAgent.receive(mt);
		if (msg != null) {
			String content = msg.getContent();
			ACLMessage reply = msg.createReply();
			reply.setPerformative(ACLMessage.CONFIRM);
			reply.setContent("Dough Notification was received");
			System.out.println(getAID().getLocalName() + "Received notification request");
			baseAgent.sendMessage(reply);
			System.out.println(content);

		}
		else {
			block();
		}
	}
}


}
