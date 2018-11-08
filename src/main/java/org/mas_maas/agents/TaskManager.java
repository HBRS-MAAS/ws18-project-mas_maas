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
public class TaskManager extends Agent {
	private String orderMessage;
	private AID [] doughMakers;
    private AID [] ovenManagers;
    private AID [] deliveryManagers;
	private boolean doughNotification = false;
	private boolean ovenNotification = false;
	private boolean deliveryNotification = false;


	protected void setup() {
		// Printout a welcome message
		System.out.println(getAID().getLocalName() + " is ready.");

		addBehaviour(new OrderRequestsServer());

		addBehaviour(new ManageOrder());

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

	private class OrderRequestsServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                orderMessage = msg.getContent();
				System.out.println(getAID().getLocalName() + " received the order" + orderMessage);
            }
            else {
                block();
            }
        }
    }  // End of inner class OrderRequestsServer

	public void getDoughMakers(Agent myAgent){
        // Update the list of seller agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("dough-maker");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(myAgent, template);
            System.out.println("Found the following doughMaker agents:");
            doughMakers = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
               doughMakers[i] = result[i].getName();
                System.out.println(doughMakers[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

	public void getOvenManagers(Agent myAgent){
        // Update the list of seller agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("oven-manager");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(myAgent, template);
            System.out.println("Found the following OvenManager agents:");
            ovenManagers = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
               ovenManagers[i] = result[i].getName();
                System.out.println(ovenManagers[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

	public void getDeliveryManagers(Agent myAgent){
        // Update the list of seller agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("delivery-manager");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(myAgent, template);
            System.out.println("Found the following DeliveryManager agents:");
            deliveryManagers = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
               deliveryManagers[i] = result[i].getName();
                System.out.println(deliveryManagers[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

	private class ManageOrder extends Behaviour {
		private int step = 0;

		public void action () {
			switch (step){
				case 0:
					addBehaviour(new ManageDough());
					block( 1800 );
					step = 1;
					break;
				case 1:
					addBehaviour(new ManageOven());
					block( 1800 );
					step = 2;
					break;

				case 2:
					addBehaviour(new ManageDelivery());
					block( 1800 );
					step = 3;

			}
		}
		public boolean done() {
            return (step == 3);
        }

	}

	private class ManageDough extends Behaviour {
		private String typeOrder;

		public void action() {
			System.out.println(getAID().getLocalName() + "talks to the Dough Maker");
			getDoughMakers(myAgent);

			// Create dough order
			typeOrder = "doughOrder";
			String doughOrderMessage;
			JSONObject doughOrder = new JSONObject();
			doughOrder.put("customer_id", "001");
			doughOrder.put("preparation_time", 10.0);
			doughOrder.put("volume", 3.0);
			doughOrder.put("resting_time", 4.0);
			doughOrder.put("product_type", 1);

			doughOrderMessage = doughOrder.toString();

			System.out.println(getAID().getName() + " places the dough order " + doughOrderMessage);

			// Place dough Order
			myAgent.addBehaviour(new PlaceOrder(doughOrderMessage, typeOrder));

			if (doughNotification)
				System.out.println("Dough production is done");
				done();

			}

		public boolean done(){
			return true;
		}

	}

	private class ManageOven extends Behaviour {
		private String typeOrder;

		public void action() {
			System.out.println(getAID().getLocalName() + "talks to the Oven Manager");
			getOvenManagers(myAgent);

			//Create baking order
			typeOrder = "bakeryOrder";
			String bakingOrderMessage;
			JSONObject bakingOrder = new JSONObject();
			bakingOrder.put("customer_id", "001");
			bakingOrder.put("product_type", 1);
			bakingOrder.put("quantity", 5);
			bakingOrder.put("backing_temperature", 100.0);
			bakingOrder.put("cooling_rate", 1.3);
			bakingOrder.put("backing_time", 45.5);
			bakingOrder.put("customer_id", "001");
			bakingOrder.put("quantity_per_slot", 10);

			bakingOrderMessage = bakingOrder.toString();

			System.out.println(getAID().getName() + " places the baking order " + bakingOrderMessage);

			// Place dough Order
			myAgent.addBehaviour(new PlaceOrder(bakingOrderMessage, typeOrder));

			if (ovenNotification)
				System.out.println("Baking is done");
				done();

			}

		public boolean done(){
			return true;
		}

	}

	private class ManageDelivery extends Behaviour {
		private String typeOrder;

		public void action() {
			System.out.println(getAID().getLocalName() + "talks to the Delivery Manager");
			getDeliveryManagers(myAgent);

			//Create delivery order
			typeOrder = "deliveryOrder";
			String deliveryOrderMessage;
			JSONObject deliveryOrder = new JSONObject();
			deliveryOrder.put("customer_id", "001");
			deliveryOrder.put("product_type", 1);
			deliveryOrder.put("quantity", 5);
			deliveryOrder.put("boxing_temperature", 15.0);
			deliveryOrder.put("cooling_rate", 1.3);
			deliveryOrder.put("quantity_per_box", 10);

			deliveryOrderMessage = deliveryOrder.toString();

		   System.out.println(getAID().getName() + " places the delivery order " + deliveryOrderMessage);

			// Place dough Order
			myAgent.addBehaviour(new PlaceOrder(deliveryOrderMessage, typeOrder));

			if (deliveryNotification)
				System.out.println("Preparation for delivery is completed");
				done();

			}

		public boolean done(){
			return true;
		}

	}

	private class PlaceOrder extends Behaviour {
        private AID senderAgent; // The agent who provides the first confirmation
        private int repliesCnt = 0; // The counter of replies from order processing agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private String orderMessage;
        private AID [] receiverAgents;
        private String typeOrder;

	    public PlaceOrder(String orderMessage, String typeOrder){
	        this.orderMessage = orderMessage;
	        this.typeOrder = typeOrder;
	        if (this.typeOrder.equals("doughOrder"))
	            this.receiverAgents = doughMakers;
	        if (this.typeOrder.equals("bakeryOrder"))
	            this.receiverAgents = ovenManagers;
	        if (this.typeOrder.equals("deliveryOrder"))
	            this.receiverAgents = deliveryManagers;
	    }

	    public void action() {
	        switch (step) {
	        case 0:
	            // Send the cfp to all receiverAgents
	            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
	            for (int i = 0; i < receiverAgents.length; ++i) {
	                cfp.addReceiver(receiverAgents[i]);
	            }
	            cfp.setContent(orderMessage);
	            cfp.setConversationId("place-order");
	            cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
	            myAgent.send(cfp);
	            // Prepare the template to get proposals
	            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("place-order"),
	                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
	            step = 1;
	            break;

	        case 1:
	            // Receive all proposals/refusals
	            ACLMessage reply = myAgent.receive(mt);
	            if (reply != null) {
	                // Reply received
	                if (reply.getPerformative() == ACLMessage.PROPOSE) {
	                    // This is an offer
	                    // Get the sender of the first confirmation message
	                    senderAgent= reply.getSender();
	                }
	                repliesCnt++;
	                if (repliesCnt >= receiverAgents.length) {
	                    // We received all replies
	                    System.out.println("Agent "+getAID().getLocalName()+ " received a confirmation from " + senderAgent.getLocalName());
	                    if (this.typeOrder.equals("doughOrder"))
	                        doughNotification = true;
						else if (this.typeOrder.equals("bakeryOrder"))
							ovenNotification = true;
						else if (this.typeOrder.equals("deliveryOrder"))
						{
							deliveryNotification = true;
							doDelete();

						}

	                    step = 2;
	                    break;
	                }
	            }
	            else {
	                block();
	            }
	            break;

	        default:
	            System.out.println("INVALID CASE!");
	            break;

	        }
	    }

	    public boolean done() {
	        return (step == 2);
	    }
	}  // End of inner class PlaceOrder
}
