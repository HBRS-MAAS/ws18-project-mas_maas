//Code based on the examples.bookTrading package in http://jade.tilab.com/download/jade/

package org.mas_maas.agents;


import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.JSONObject;
import org.json.JSONArray;
// import org.json.JSONParser;

@SuppressWarnings("serial")
public class OrderProcessing extends Agent {

    // The list of known Customer Agents
    private AID [] Customers;

    // Agent initialization
    protected void setup() {

        System.out.println(getAID().getLocalName() + " is ready.");

        registerOrderProcessing();

        // Add the behavior serving queries from customer agents
        addBehaviour(new OfferRequestsServer());

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
                    System.out.println("There are no customers... terminating");
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

        sd.setType("customer");
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

    /**
       Inner class OfferRequestsServer.
       This is the behaviour used by OrderProcessing agents to serve incoming orders from Customers.
       Sends a confirmation message to the customer.
     */
    private class OfferRequestsServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String order = msg.getContent();
                ACLMessage reply = msg.createReply();

                // Create confirmation message
                String confirmationMessage;
                JSONObject confirmation = new JSONObject();
                confirmation.put("accept", true);
                // confirmation.put("customer_id","001");
                confirmationMessage = confirmation.toString();
                // Send confirmation message
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(confirmationMessage);

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
