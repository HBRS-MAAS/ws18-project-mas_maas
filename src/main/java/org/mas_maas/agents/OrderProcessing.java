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
import jade.core.behaviours.Behaviour;
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
    private AID [] customers;
    // The list of known OrderProcessing Agents
    private AID [] schedulerAgents;
    private int nIncomingOrders = 0;
    private int nSentOrders = 0;
    // orderMessage received from the customer. (In this case just one. TODO: have an array of received orderMessages)
    private String orderMessage;

    // Agent initialization
    protected void setup() {

        System.out.println(getAID().getLocalName() + " is ready.");

        registerOrderProcessing();

        // Add the behavior serving queries from customer agents
        addBehaviour(new OrderRequestsServer());

         // Add a TickerBehaviour to send an order to the scheduler
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {

                if (nIncomingOrders > 0) {
                    // Check the number of orders placed so far
                    if (checkNOrders())
                        stop();

                    System.out.println(getAID().getLocalName() + " is sending an order to the scheduler");

                    // Update order scheduler agents
                    getSchedulerAgents(myAgent);


                    // Perform the request
                    myAgent.addBehaviour(new PlaceOrder(orderMessage));
                    nIncomingOrders -= 1;
                }
            }

        } );


        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
        // Add a TickerBehaviour to look for customers
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                getCustomers(myAgent);
                if(customers.length == 0){
                    System.out.println("There are no customers... terminating");
                    addBehaviour(new shutdown());
                }
            }

        } );

    }

    public boolean checkNOrders(){
        if(nIncomingOrders == nSentOrders)
        {
            System.out.println(getAID().getLocalName() + " has placed " + nSentOrders + " orders");
            // Stop the behaviour
            return true;
        }
        return false;
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
        // Get agents who provide the service "customer"
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("customer");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(myAgent, template);
            customers = new AID [result.length];
            for (int i = 0; i < result.length; ++i) {
                customers[i] = result[i].getName();
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

    }

    public void getSchedulerAgents(Agent myAgent){
        // Get agents who provide the service "scheduling"
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("scheduling");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(myAgent, template);
            System.out.println("Found the following scheduling agents:");
            schedulerAgents= new AID [result.length];
            for (int i = 0; i < result.length; ++i) {
                schedulerAgents[i] = result[i].getName();
                System.out.println(schedulerAgents[i].getName());
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
                orderMessage = msg.getContent();
                ACLMessage reply = msg.createReply();
                System.out.println(getAID().getLocalName() + " received an order request");
                nIncomingOrders += 1;

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

    /**
       Inner class PlaceOrder.
       This is the behaviour used by an OrderProcessing agent to place an order
     */
    private class PlaceOrder extends Behaviour {
        private AID scheduler; // The agent who provides the first confirmation
        private int repliesCnt = 0; // The counter of replies from order processing agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private String orderMessage;

        public PlaceOrder(String orderMessage){
            this.orderMessage = orderMessage;
        }

        public void action() {
            switch (step) {
            case 0:
                // Send the cfp to all schedulerAgents
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < schedulerAgents.length; ++i) {
                    cfp.addReceiver(schedulerAgents[i]);
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
                // Receive all proposals/refusals from seller agents
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        // This is an offer
                        // Get the sender of the first confirmation message
                        scheduler = reply.getSender();
                    }
                    repliesCnt++;
                    if (repliesCnt >= schedulerAgents.length) {
                        // We received all replies
                        System.out.println("Agent "+getAID().getLocalName()+ " received a confirmation from " + reply.getSender().getLocalName());
                        nSentOrders +=1;
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
