package org.mas_maas.agents;

import org.json.JSONArray;
// import org.json.JSONParser;
import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class Customer extends Agent {

    // The list of known OrderProcessing Agents
    private AID [] orderProcessingAgents;
    private int nOrders = 1;
    private int nOrdersPlaced = 0;

    protected void setup() {
        // Welcome message
        System.out.println(getAID().getLocalName() + " is ready.");

        // Create order message
        String orderMessage;
        JSONObject order = new JSONObject();
        order.put("customer_id","001");
        order.put("order_date","12.04");
        order.put("delivery_date", "13.04");

        JSONArray list_products = new JSONArray();
        JSONObject products =  new JSONObject();
        products.put("Bagel", 2);
        products.put("Berliner", 5);
        list_products.put(products);

        order.put("list_products", list_products);
        orderMessage = order.toString();

        System.out.println(getAID().getName() + " will place the order " + orderMessage);

        // Register the customer in the yellow pages with the service "customer"
        registerCustomer();

        // Add a TickerBehaviour for the order message
        addBehaviour(new TickerBehaviour(this, 100) {
            protected void onTick() {
                // Check the number of orders placed so far
                if(nOrdersPlaced == nOrders){
                    System.out.println(getAID().getLocalName() + " has placed " + nOrdersPlaced + " orders");
                    stop();
                }
                else{
                    System.out.println(getAID().getLocalName() + " is placing an order");
                    // Update order processing agents
                    getOrderProcessingAgents(myAgent);
                    // Perform the request
                    myAgent.addBehaviour(new PlaceOrder(orderMessage));
                }

            }

        } );
        addBehaviour(new ReceiveGoods());

        try {
             Thread.sleep(3000);
         } catch (InterruptedException e) {
             //e.printStackTrace();
         }
    }

    public boolean checkNOrders(){
        if(nOrdersPlaced == nOrders)
        {
            System.out.println(getAID().getLocalName() + " has placed " + nOrdersPlaced + " orders");
            return true;
        }
        return false;
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
        // Register the customer service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("customer");
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
            System.out.println("Found the following order processing agents:");
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

    /**
       Inner class PlaceOrder.
       This is the behaviour used by a Customer to place an order
     */
    private class PlaceOrder extends Behaviour {
        private AID orderProcesser; // The agent who provides the first confirmation
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
                // Send the cfp to all OrderProcessingAgents
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < orderProcessingAgents.length; ++i) {
                    cfp.addReceiver(orderProcessingAgents[i]);
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
                        orderProcesser = reply.getSender();
                    }
                    repliesCnt++;
                    if (repliesCnt >= orderProcessingAgents.length) {
                        // We received all replies
                        System.out.println("Agent "+getAID().getLocalName()+ " received a confirmation from " + orderProcesser.getLocalName());
                        nOrdersPlaced +=1;
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

    private class ReceiveGoods extends CyclicBehaviour {
        String bakedGoodMessage;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                bakedGoodMessage = msg.getContent();
                System.out.println(getAID().getLocalName() + " received" + bakedGoodMessage);
                doDelete();
            }
            else {
                block();
            }
        }
    }  // End of inner class OrderRequestsServer
}
