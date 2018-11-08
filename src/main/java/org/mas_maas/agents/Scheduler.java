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
public class Scheduler extends Agent {

    private boolean incomingOrder = false;
    private int nTaskManagers= 0;
    // orderMessage received from the OrderProcessingAgent. (In this case just one. TODO: have an array of received orderMessages)
    private String orderMessage;

    // Agent initialization
    protected void setup() {

        System.out.println(getAID().getLocalName() + " is ready.");

        registerScheduler();

        // Add the behavior serving queries from order processer agents
        addBehaviour(new OrderRequestsServer());

        // Add a TickerBehaviour to create a Task Manager agent if an order is received
       addBehaviour(new TickerBehaviour(this, 2000) {
           protected void onTick() {

               if (incomingOrder) {
                   System.out.println("Creating TaskManager");
                   incomingOrder = false;

               }
           }

       } );

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }

    }

    public void registerScheduler(){
        // Register the scheduler service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("scheduling");
        sd.setName("JADE-bakery");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
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
       Inner class OrderRequestsServer.
       This is the behaviour used by OrderProcessing agents to serve incoming orders from Customers.
       Sends a confirmation message to the customer.
     */
    private class OrderRequestsServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String order = msg.getContent();
                ACLMessage reply = msg.createReply();
                System.out.println(getAID().getLocalName() + " received an order request");
                incomingOrder = true;

                // Create confirmation message
                String confirmationMessage;
                JSONObject confirmation = new JSONObject();
                confirmation.put("accept", true);
                // TODO put customer id in the confirmation message
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
    }  // End of inner class OrderRequestsServer
}
