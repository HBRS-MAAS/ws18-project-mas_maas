package org.mas_maas.agents;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import org.json.JSONObject;
import org.json.JSONArray;


@SuppressWarnings("serial")
public class DoughMaker extends Agent {
	private String doughOrderMessage;

	protected void setup() {
	// Printout a welcome message
		System.out.println(getAID().getLocalName() + " is ready.");
		registerDoughMaker();
		// Add the behavior serving order requests
        addBehaviour(new OrderRequestsServer());

        try {
 			Thread.sleep(3000);
 		} catch (InterruptedException e) {
 			//e.printStackTrace();
 		}
		//addBehaviour(new shutdown());

	}
	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

	public void registerDoughMaker(){
        // Register the customer service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("dough-maker");
        sd.setName("JADE-bakery");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

	private class OrderRequestsServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                doughOrderMessage = msg.getContent();
                ACLMessage reply = msg.createReply();
                System.out.println(getAID().getLocalName() + " received the doughOrderMessage" + doughOrderMessage);

                // Create notification message
                String notificationMessage;
                JSONObject notification = new JSONObject();
                notification.put("order_finished", true);
                // TODO put customer id and product type in the confirmation message
                notificationMessage = notification.toString();
                // Send confirmation message
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(notificationMessage);

                myAgent.send(reply);
				doDelete();
            }
            else {
                block();
            }
        }
    }  // End of inner class OrderRequestsServer

}
