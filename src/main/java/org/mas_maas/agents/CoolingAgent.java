package org.mas_maas.agents;

import java.util.concurrent.atomic.AtomicBoolean;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.CoolingRequest;
import org.mas_maas.messages.LoadingBayMessage;

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
    private AID [] packagingInterfaceAgents;

    private AtomicBoolean coolingInProcess = new AtomicBoolean(false);

    private String productName;
    private int quantity;
    private int boxingTemp;

    private int coolingCounter;

    protected void setup() {
        super.setup();

        System.out.println(getAID().getLocalName() + " is ready.");

        // Register CoolingMachine Agent to the yellow Pages
        this.register("CoolingRack", "JADE-bakery");

        // Get Agents AIDS
        this.getBakingManagerAIDs();

        this.getPackagingInterfaceManagerAIDs();

        coolingCounter = 0;
        // Time tracker behavior
        addBehaviour(new timeTracker());

        // Creating receive cooling requests behaviour
        addBehaviour(new ReceiveCoolingRequests());

    }
    
    public LoadingBayMessage createLoadingBayMessage(String productName, int quantity) {
    	LoadingBayMessage loadingBayMessage = new LoadingBayMessage();
    	loadingBayMessage.addProduct(productName, quantity);
    	
    	return loadingBayMessage;
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }else{
                if (coolingInProcess.get()){
                    coolingCounter++;
                    System.out.println("-------> Cooler Clock-> " + baseAgent.getCurrentHour());
                    System.out.println("-------> Cooler Counter -> " + coolingCounter);
                }
            }
            baseAgent.finished();
        }
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

    public void getPackagingInterfaceManagerAIDs() {
        /*
        Object the AID of all the dough-manager agents found
        */
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Packaging-interface");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following packaging-interface agents:");
            packagingInterfaceAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                packagingInterfaceAgents[i] = result[i].getName();
                System.out.println(packagingInterfaceAgents[i].getName());
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
                System.out.println("Cooling request contains -> " + content);
                CoolingRequest coolingRequest = JSONConverter.parseCoolingRequest(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Cooling request was received");
                reply.setConversationId("cooling-request-reply");
                baseAgent.sendMessage(reply);

                Float coolingTime = coolingRequest.getCoolingTime();
                productName = coolingRequest.getProductName();
                quantity = coolingRequest.getQuantity();
                // boxingTemp = coolingRequest.getBoxingTemp();

                addBehaviour(new Cooling(coolingTime, productName, quantity));

            }

            else {
                block();
            }
        }
    }

    // performs Cooling process
    private class Cooling extends Behaviour {
        private Float coolingTime;
        private int option = 0;
        private String productName;
        private int quantity;

        public Cooling(Float coolingTime, String productName, int quantity){
            this.coolingTime = coolingTime;
            this.productName = productName;
            this.quantity = quantity;
            System.out.println("----> "+ getAID().getLocalName() + " Cooling for " + coolingTime);
            coolingInProcess.set(true);
        }

        public void action(){
            if (coolingCounter >= coolingTime){
                coolingInProcess.set(false);
                coolingCounter = 0;
                LoadingBayMessage loadingBayMessage = createLoadingBayMessage(productName, quantity);
				
                Gson gson = new Gson();
                String loadingBayMessageString = gson.toJson(loadingBayMessage);
                
                addBehaviour(new SendLoadingBayMessage(packagingInterfaceAgents, loadingBayMessageString));
                // this.done();
            }

        }
        public boolean done(){
            if (coolingInProcess.get()){
                return false;
            }else{
                return true;
            }
        }
    }

    // Send a loadingBayMessage msg to the doughManager agents
     private class SendLoadingBayMessage extends Behaviour {
         private AID [] packagingInterfaceAgents;
         private String loadingBayMessageString;
         private MessageTemplate mt;
         private int option = 0;
         // private Vector<ProductPair> products;
         private Gson gson = new Gson();
         // private LoadingBayMessage loadingBayMessage = new LoadingBayMessage(guids, productName);
         // private String loadingBayMessageString = gson.toJson(loadingBayMessage);

         public SendLoadingBayMessage(AID []packagingInterfaceAgents, String loadingBayMessageString){

             this.packagingInterfaceAgents = packagingInterfaceAgents;
             this.loadingBayMessageString = loadingBayMessageString;
         }

         public void action() {
             switch (option) {
                 case 0:

                     ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                     System.out.println("---->  Sending a loadingBay Message");

                     System.out.println("-----> LoadingBayMessage " + loadingBayMessageString);

                     msg.setContent(loadingBayMessageString);

                     msg.setConversationId("loadingBay-message");

                     // Send loadingBayMessage msg to packagingInterfaceAgents
                     for (int i = 0; i < packagingInterfaceAgents.length; i++){
                         msg.addReceiver(packagingInterfaceAgents[i]);
                     }

                     // msg.setReplyWith("msg" + System.currentTimeMillis());

                     baseAgent.sendMessage(msg);

                     mt = MessageTemplate.MatchConversationId("loadingBay-message");

                     option = 1;

                     System.out.println(getAID().getLocalName() + " Sent loadingBayMessage");

                     break;

                 case 1:
                     ACLMessage reply = baseAgent.receive(mt);

                     if (reply != null) {

                         if (reply.getPerformative() == ACLMessage.CONFIRM) {
                             System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                             option = 2;
                         }
                     }
                     else {
                         block();
                     }
                     break;

                 default:
                     break;
             }
         }

         public boolean done() {
             if (option == 2) {
                 baseAgent.finished();
                 myAgent.doDelete();
                 return true;
             }

            return false;
        }
     }
}
