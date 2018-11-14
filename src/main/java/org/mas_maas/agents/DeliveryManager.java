package org.mas_maas.agents;
import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


@SuppressWarnings("serial")
public class DeliveryManager extends Agent {
    private String deliveryOrderMessage;

    protected void setup() {
    // Printout a welcome message
        System.out.println(getAID().getLocalName() + " is ready.");
        registerDeliveryManager();
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

    public void registerDeliveryManager(){
        // Register the customer service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("delivery-manager");
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
                deliveryOrderMessage = msg.getContent();
                ACLMessage reply = msg.createReply();
                System.out.println(getAID().getLocalName() + " received the delivery order message" + deliveryOrderMessage);

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
                addBehaviour(new DeliverOrder());
            }
            else {
                block();
            }
        }
    }  // End of inner class OrderRequestsServer

    private class DeliverOrder extends Behaviour {
        private AID [] customers;

        public void action() {
            // Sends baked goods message to the customer
            //TODO get the AID of the customer from the AID
            //     get list of products from the order message

            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();

            sd.setType("customer");
            template.addServices(sd);
            try {
                DFAgentDescription [] result = DFService.search(myAgent, template);

                customers= new AID [result.length];

                for (int i = 0; i < result.length; ++i) {
                    customers[i] = result[i].getName();
                }

            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            // Create bakedGood message
            String bakedGoodMessage;
            JSONObject bakedGoods = new JSONObject();
            bakedGoods.put("customer_id","001");

            JSONArray list_products = new JSONArray();
            JSONObject products =  new JSONObject();
            products.put("Bagel", 2);
            products.put("Berliner", 5);
            list_products.put(products);

            bakedGoods.put("list_products", list_products);
            bakedGoodMessage = bakedGoods.toString();

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent(bakedGoodMessage);
            msg.addReceiver(customers[0]);

            send(msg);
            System.out.println(getLocalName()+" sent order message" + bakedGoodMessage + "to customer");
            done();

        }
        public boolean done() {
            return true;
        }
    }
}
