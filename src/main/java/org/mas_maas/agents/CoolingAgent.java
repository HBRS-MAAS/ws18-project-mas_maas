package org.mas_maas.agents;

import java.util.Vector;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.LoadingBayMessage;
import org.mas_maas.messages.CoolingRequest;

import org.mas_maas.objects.Bakery;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CoolingAgent extends BaseAgent {
    private AID [] bakingManagerAgents;

    private String productName;
    private int quantity;
    private int boxingTemp;

    protected void setup() {
        super.setup();

        System.out.println(getAID().getLocalName() + " is ready.");

        // Register CoolingMachine Agent to the yellow Pages
        this.register("CoolingRack", "JADE-bakery");

        // Get Agents AIDS
        this.getBakingManagerAIDs();

        // Creating receive cooling requests behaviour
        addBehaviour(new ReceiveCoolingRequests());

    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getBakingManagerAIDs() {
        /*
        Object the AID of all the dough-manager agents found
        */
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Baking-manager");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Baking-manager agents:");
            bakingManagerAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                bakingManagerAgents[i] = result[i].getName();
                System.out.println(bakingManagerAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // Receiving Cooling requests behavior
    private class ReceiveCoolingRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("cooling-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                System.out.println("-------> " + getAID().getLocalName() + " received cooling requests from " + msg.getSender());

                String content = msg.getContent();

                CoolingRequest coolingRequest = JSONConverter.parseCoolingRequest(content);

                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.CONFIRM);

                reply.setContent("Cooling request was received");

                baseAgent.sendMessage(reply);

                Float coolingTime = coolingRequest.getCoolingTime();

                productName = coolingRequest.getProductName();
                quantity = coolingRequest.getQuantity();
                boxingTemp = coolingRequest.getBoxingTemp();

                addBehaviour(new Cooling(coolingTime));

            }

            else {
                block();
            }
        }
    }

    // performs Cooling process
    private class Cooling extends Behaviour {
        private Float coolingTime;
        private Float coolingCounter = (float) 0;
        private int option = 0;

        public Cooling(Float coolingTime){
            this.coolingTime = coolingTime;
            System.out.println("----> "+ getAID().getLocalName() + " Cooling for " + coolingTime);
        }

        public void action(){
            if (getAllowAction()){
                while(coolingCounter < coolingTime){
                    coolingCounter++;
                    System.out.println("----> " + getAID().getLocalName() + " Cooling counter " + coolingCounter);
                }
                // addBehaviour(new SendLoadingBayMessage());
                this.done();
            }

        }
        public boolean done(){
            baseAgent.finished();
            return true;
        }
    }

    // Send a loadingBayMessage msg to the doughManager agents
    // private class SendLoadingBayMessage extends Behaviour {
    //     private AID [] bakingManagerAgents;
    //     private MessageTemplate mt;
    //     private int option = 0;
    //     // private Vector<ProductPair> products;
    //     private Gson gson = new Gson();
    //     // private LoadingBayMessage loadingBayMessage = new LoadingBayMessage(guids, productName);
    //     // private String loadingBayMessageString = gson.toJson(loadingBayMessage);
    //
    //     public SendLoadingBayMessage(AID [] bakingManagerAgents){
    //
    //         this.bakingManagerAgents = bakingManagerAgents;
    //     }
    //
    //     public void action() {
    //         switch (option) {
    //             case 0:
    //
    //                 ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    //
    //                 System.out.println("-----> Cooling notification string " + loadingBayMessageString);
    //
    //                 msg.setContent(loadingBayMessageString);
    //
    //                 msg.setConversationId("loadingBay-message");
    //
    //                 // Send loadingBayMessage msg to bakingManagerAgents
    //                 for (int i = 0; i < bakingManagerAgents.length; i++){
    //                     msg.addReceiver(bakingManagerAgents[i]);
    //                 }
    //
    //                 // msg.setReplyWith("msg" + System.currentTimeMillis());
    //
    //                 baseAgent.sendMessage(msg);
    //
    //                 mt = MessageTemplate.MatchConversationId("loadingBay-message");
    //
    //                 option = 1;
    //
    //                 System.out.println(getAID().getLocalName() + " Sent loadingBayMessage");
    //
    //                 break;
    //
    //             case 1:
    //                 ACLMessage reply = baseAgent.receive(mt);
    //
    //                 if (reply != null) {
    //
    //                     if (reply.getPerformative() == ACLMessage.CONFIRM) {
    //                         System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
    //                         option = 2;
    //                     }
    //                 }
    //                 else {
    //                     block();
    //                 }
    //                 break;
    //
    //             default:
    //                 break;
    //         }
    //     }
    //
    //     public boolean done() {
    //         if (option == 2) {
    //             baseAgent.finished();
    //             myAgent.doDelete();
    //             return true;
    //         }
    //
    //        return false;
    //    }
    // }

}
