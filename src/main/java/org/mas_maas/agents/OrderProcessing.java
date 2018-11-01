//Code based on the examples.bookTrading package in http://jade.tilab.com/download/jade/

package org.mas_maas.agents;


import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.*;

public class OrderProcessing extends Agent {
    // The catalogue of breads
    private Hashtable catalogueBreads;

    // The list of known Customer Agents
    private AID [] Customers;

    // Agent initialization
    protected void setup() {

		System.out.println(getAID().getLocalName()+" is ready.");

        initializeCatalogue();

        System.out.println(getAID().getLocalName()+ "(bread, price): " + catalogueBreads);

        registerOrderProcessing();

        // Add the behaviour serving queries from customer agents
		addBehaviour(new OfferRequestsServer());

		// Add the behaviour serving purchase orders from buyer agents
		addBehaviour(new PurchaseOrdersServer());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
        // Add a TickerBehaviour to look for customers
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                getCustomers(myAgent);
                if(Customers.length == 0){
                    System.out.println("There are no buyers... terminating");
                    addBehaviour(new shutdown());
                }
            }

        } );

    }

    public void registerOrderProcessing(){
        // Register the order-processing service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("order-processing");
		sd.setName("JADE-bakery");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
    }

    public void getCustomers(Agent myAgent){
		// Update the list of buyer agents
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("bread-buying");
		template.addServices(sd);
		try {
			DFAgentDescription [] result = DFService.search(myAgent, template);
			Customers = new AID [result.length];
			for (int i = 0; i < result.length; ++i) {
				Customers[i] = result[i].getName();
			}
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

	}

    protected void takeDown() {
        // Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}

    protected void initializeCatalogue(){
        List<String> breads = new Vector<>();
        Random rand = new Random();

        breads.add("Bagel");
		breads.add("Donut");
		breads.add("Berliner");
		breads.add("Baguette");

        // Create the catalogue of books
        catalogueBreads = new Hashtable();

		// Fill in the catalogue of breads.
        // The price is an int random number between 1 and 3
        for (int i=0; i<breads.size(); i++){
            int price = rand.nextInt(3) +1;
			catalogueBreads.put(breads.get(i),price);
		}
    }


    /**
	   Inner class OfferRequestsServer.
	   This is the behaviour used by OrderProcessing agents to serve incoming orders from Customers.
       If the bread requested is in the catalogue, the OrderProcessing replies
	   with a PROPOSE message specifying the price. Otherwise a REFUSE message is
	   sent back.
	 */
	private class OfferRequestsServer extends CyclicBehaviour {

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String order = msg.getContent();
				ACLMessage reply = msg.createReply();

                // Check if ordered bread exists in catalogueBreads
                boolean breadIncatalogueBreads = catalogueBreads.containsKey(order);

                Integer price = (Integer) 0;

                // Get the price for the bread
                if  (breadIncatalogueBreads){
                    price = (Integer) catalogueBreads.get(order);
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					// The requested bread is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

    /**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by OrderProcessingr agents to serve incoming
	   offer acceptances (i.e. purchase orders) from Customer agents.

       The OrderProcessing replies with an INFORM message to notify the buyer that the
	   purchase has been sucesfully completed.
	 */
	private class PurchaseOrdersServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
				String order = msg.getContent();
				ACLMessage reply = msg.createReply();



                reply.setPerformative(ACLMessage.INFORM);
				System.out.println("Confirm order "+order+"from"+ msg.getSender().getName());

				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour{
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
			try {
			    myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
			    myAgent.send(shutdownMessage);
			}
			catch (Exception e) {
			    //LOGGER.error(e);
			}

		}
	}
}
