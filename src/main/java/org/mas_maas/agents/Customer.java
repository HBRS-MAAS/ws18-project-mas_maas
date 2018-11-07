package org.mas_maas.agents;

import java.util.List;
import java.util.Random;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


@SuppressWarnings("serial")
public class Customer extends Agent {
    // catalogue of breads from which the customer can select from
    private List<String> catalogueBreads;

    // Bread for which the agent will place an order
    private List<String> targetBreads;
    private int nTargetBreads = 1;

    // Orders placed by the customer
    private List<String> ordersPlaced;
    // The list of known OrderProcessing Agents
    private AID [] orderProcessingAgents;

    protected void setup() {
        // Welcome message
        System.out.println(getAID().getLocalName() + " is ready.");

        initializecatalogueBreads();
        initializeTargetBreads();

        ordersPlaced = new Vector<>();

        System.out.println(getAID().getName() + " will place an order for  " + targetBreads);

        // Register the customer in the yellow pages with the service "breadbuying"
        registerCustomer();

        // Add a TickerBehaviour for an order
        for (String targetBread : targetBreads) {
            addBehaviour(new TickerBehaviour(this, 2000) {
                protected void onTick() {
                    System.out.println(getAID().getLocalName() + "is placing an order for " + targetBread);

                    // Update order processing agents
                    getOrderProcessingAgents(myAgent);

                    if(ordersPlaced.contains(targetBread)){
                        System.out.println(getAID().getLocalName() + " has placed and order for" + targetBread);
                        printordersPlaced();
                        // Check the number of orders placed so far
                        checkNOrders();
                        // Stop the TickerBehaviour that is trying to place an order for targetBread
                        stop();
                    }
                    else{
                        // Perform the request
                        myAgent.addBehaviour(new PlaceOrder(targetBread));

                    }
                }

            } );

        }

        try {
             Thread.sleep(3000);
         } catch (InterruptedException e) {
             //e.printStackTrace();
         }
    }

    public void checkNOrders(){
        if(ordersPlaced.size() == nTargetBreads)
        {
            System.out.println(getAID().getLocalName() + " has places " + ordersPlaced.size() + " orders");
            // Stop this agent
            doDelete();
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

    public void registerCustomer(){
        // Register the bread-buying service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("bread-buying");
        sd.setName("JADE-bakery");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public void getOrderProcessingAgents(Agent myAgent){
        // Update the list of seller agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("order-processing");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(myAgent, template);
            System.out.println("Found the following seller agents:");
            orderProcessingAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                orderProcessingAgents[i] = result[i].getName();
                System.out.println(orderProcessingAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

    }

    public void printordersPlaced(){
        System.out.println("Agent"+ getAID().getLocalName()+" placed an order for:");
        System.out.println(ordersPlaced);
    }

    public void initializecatalogueBreads(){
        catalogueBreads = new Vector<>();
        catalogueBreads.add("Bagel");
        catalogueBreads.add("Donut");
        catalogueBreads.add("Berliner");
        catalogueBreads.add("Baguette");
    }

    protected void initializeTargetBreads(){
        targetBreads = new Vector<>();
        Random rand = new Random();

        // Get a random index of the catalogueBreads until the target books has nTargetBreads
        while(targetBreads.size()< nTargetBreads){
            int randomIndex = rand.nextInt(catalogueBreads.size());
            boolean titleInTargetBreads = targetBreads.contains(catalogueBreads.get(randomIndex));
            if (!titleInTargetBreads)
                targetBreads.add(catalogueBreads.get(randomIndex));
        }

    }

    /**
       Inner class PlaceOrder.
       This is the behaviour used by a Customer to place an order
     */
    private class PlaceOrder extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer
        private int bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private String targetBread;

        public PlaceOrder(String targetBread){
            this.targetBread = targetBread;
        }

        public void action() {
            switch (step) {
            case 0:
                // Send the cfp to all OrderProcessingAgents
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < orderProcessingAgents.length; ++i) {
                    cfp.addReceiver(orderProcessingAgents[i]);
                }
                cfp.setContent(targetBread);
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
                        int price = Integer.parseInt(reply.getContent());
                        if (bestSeller == null || price < bestPrice) {
                            // This is the best offer at present
                            bestPrice = price;
                            bestSeller = reply.getSender();
                        }
                    }
                    repliesCnt++;
                    if (repliesCnt >= orderProcessingAgents.length) {
                        // We received all replies
                        step = 2;
                    }
                }
                else {
                    block();
                }
                break;

            case 2:
                // Send the purchase order to the seller that provided the best offer
                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                order.addReceiver(bestSeller);
                order.setContent(targetBread);
                order.setConversationId("place-order");
                order.setReplyWith("order"+System.currentTimeMillis());
                myAgent.send(order);
                // Prepare the template to get the purchase order reply
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("place-order"),
                        MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                step = 3;
                break;

            case 3:
                // Receive the purchase order reply
                reply = myAgent.receive(mt);
                if (reply != null) {
                    // Purchase order reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        // Purchase successful. We can terminate
                        System.out.println("Agent "+getAID().getLocalName()+ " successfully purchased "+ targetBread+ " from agent "+reply.getSender().getLocalName());
                        System.out.println("Bought at price = "+bestPrice);
                        ordersPlaced.add(targetBread);
                        //myAgent.doDelete();
                    }
                    else {
                        System.out.println("Attempt failed: requested book already sold.");
                    }

                    step = 4;
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
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: "+targetBread+" not available");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class PlaceOrder
}
