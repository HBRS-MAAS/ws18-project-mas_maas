package org.mas_maas.agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.maas.utils.Time;

import org.maas.JSONConverter;
import org.maas.messages.BakingNotification;
import org.maas.messages.BakingRequest;
import org.maas.Objects.Bakery;
import org.maas.Objects.Batch;
import org.maas.Objects.Equipment;
import org.maas.Objects.Oven;
import org.maas.Objects.OvenSlot;
import org.maas.Objects.ProductMas;

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

import org.maas.agents.BaseAgent;

public class OvenAgent extends BaseAgent {
    private AID bakingManagerAgent;

    private AtomicBoolean bakingInProcess = new AtomicBoolean(false);
    private AtomicBoolean heatingUp = new AtomicBoolean(false);
    private AtomicBoolean coolingDown = new AtomicBoolean(false);
    private AtomicBoolean bakedFullOrder = new AtomicBoolean(false);
    private AtomicBoolean needHeating = new AtomicBoolean(false);
    private AtomicBoolean needCooling = new AtomicBoolean(false);
    private AtomicInteger messageProcessing = new AtomicInteger(0);
    private AtomicInteger bakingCounter = new AtomicInteger(0);
    private AtomicBoolean isInProductionTime = new AtomicBoolean (false);

    private Oven oven;

    private Bakery bakery;
    private String bakeryId;

    private String ovenName;
    private String bakingManagerName;
    private String scenarioPath;

    private int bakingTemp;
    private String productType;
    private Vector<String> guids;
    private Vector<Integer> productQuantities;

    private Vector<ProductMas> products;


    protected void setup() {
        super.setup();

        Object[] args = getArguments();

        if(args != null && args.length > 0){
            this.oven = (Oven) args[0];
            this.ovenName = (String) args[1];
            this.bakingManagerName = (String) args[2];
            this.scenarioPath = (String) args[3];
            this.bakeryId = (String) args[4];
        }

        //Read the scenario file and get the bakery with this.bakeryId
        getBakery(scenarioPath);

        this.getBakingManagerAID();

        System.out.println("Hello! " + getAID().getLocalName() + " is ready." + "its BakingManager is: " +
            bakingManagerName);

        this.register(this.ovenName, "JADE-bakery");

        oven.setAvailable(true);

        // Time tracker behavior
        addBehaviour(new timeTracker());
        addBehaviour(new ReceiveProposalRequests());
        // Creating receive bakingRequests behaviour
        addBehaviour(new ReceiveBakingRequests());
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getBakingManagerAID() {
        bakingManagerAgent = new AID (bakingManagerName, AID.ISLOCALNAME);
    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }

            // if (bakingInProcess.get()){
            //     int curBakingCount = bakingCounter.incrementAndGet();
            //     System.out.println("=========================================" );
            //     System.out.println("-------> Oven Clock-> " + baseAgent.getCurrentHour());
            //     System.out.println("-------> baking Counter -> " + curBakingCount);
            //     System.out.println("=========================================" );
            // }
            //
            // else{
            //     if (needHeating.get()){
            //         // System.out.println("------->  Allowing heat");
            //         heatingUp.set(true);
            //     }
            //     if (needCooling.get()){
            //         // System.out.println("-------> Winter is coming");
            //         coolingDown.set(true);
            //     }
            //
            // }

            if (messageProcessing.get() <= 0)
            {
                // Production time is from midnight to lunch (from 00.00 hrs to 12 hrs)
               if ((baseAgent.getCurrentTime().greaterThan(new Time(baseAgent.getCurrentDay(), 0, 0)) ||

                       baseAgent.getCurrentTime().equals(new Time(baseAgent.getCurrentDay(), 0, 0))) &&

                       baseAgent.getCurrentTime().lessThan(new Time(baseAgent.getCurrentDay(), 12, 0)))
                {

                   isInProductionTime.set(true);
                }
                else{

                    isInProductionTime.set(false);
                }
                baseAgent.finished();
            }
        }
    }

    public void getBakery(String scenarioPath){
        String jsonDir = scenarioPath;
        try {
            // System.out.println("Working Directory = " + System.getProperty("user.dir"));
            String bakeryFile = new Scanner(new File(jsonDir + "bakeries.json")).useDelimiter("\\Z").next();
            Vector<Bakery> bakeries = JSONConverter.parseBakeries(bakeryFile);

            for (Bakery bakery : bakeries){
                if (bakery.getGuid().equals(bakeryId)){
                    this.bakery = bakery;
                }
            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Get list of products
        products = bakery.getProducts();

    }

    private class ReceiveProposalRequests extends CyclicBehaviour{
        public void action(){
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("baking-request"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null){
                String content = msg.getContent();
                // System.out.println(" (1) " + getAID().getLocalName() + " has received a CFP from "
                //     + msg.getSender().getName() + " for " + content);

                ACLMessage reply = msg.createReply();
                if (oven.isAvailable()){
                	//System.out.println(getAID().getLocalName() + " is available");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Hey I am free, do you wanna use me ;)? " + content);
                }else{
                	// System.out.println(getAID().getLocalName() + " is unavailable");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Sorry, I am married potato :c " + content);
                }
                baseAgent.sendMessage(reply);
                messageProcessing.decrementAndGet();
            }

            else{
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    // Receiving Baking requests behavior
    private class ReceiveBakingRequests extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            // MessageTemplate mt = MessageTemplate.and(
            //     MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
            //     MessageTemplate.MatchConversationId("baking-request"));
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                ACLMessage new_msg = msg.createReply();

                if (!oven.isAvailable()){


                    new_msg.setPerformative(ACLMessage.CANCEL);
                    new_msg.setContent("Oven is taken");
                    // System.out.println("(4.2) " + getAID().getLocalName() + " cannot perform Baking for "
                    //     + msg.getSender() + " baking information -> " + msg.getContent());
                }
                else{
                    oven.setAvailable(false);
                    String content = msg.getContent();
                    System.out.println("(4) " + getAID().getLocalName() + " WILL perform Baking for "
                        + msg.getSender() + " baking information -> " + content);

                    BakingRequest bakingRequest = JSONConverter.parseBakingRequest(content);

                    new_msg.setPerformative(ACLMessage.AGREE);
                    new_msg.setContent("Baking request was received " + content);

                    bakingTemp = bakingRequest.getBakingTemp();
                    Float bakingTime = bakingRequest.getBakingTime();
                    productType = bakingRequest.getProductType();
                    guids = bakingRequest.getGuids();
                    productQuantities = bakingRequest.getProductQuantities();

                    System.out.println("---> Baking request " + bakingRequest);

                    // messageInProcress.set(false);
                    addBehaviour(new Baking(bakingTime));
                    // try {
                    //     Thread.sleep(3000);
                    // } catch (InterruptedException e) {
                    //     e.printStackTrace();
                    // }
                }
                baseAgent.sendMessage(new_msg);
                messageProcessing.decrementAndGet();
            }

            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    private class Baking extends Behaviour {
        private Float bakingTime;
        private int option = 0;
        private boolean[] slotsReady;
        private int ovenAvailable;
        private int slotAvailable;
        // private Vector<OvenSlot> slots;
        private int productPerSlot;
        private int productIdx = 0;
        private int slotIdx = 0;
        private int requestedQuantity = 0;
        private Vector<OvenSlot> chosenSlots = new Vector<OvenSlot> ();

        public Baking(Float bakingTime){
            this.bakingTime = bakingTime;
            System.out.println("=========================================" );
            System.out.println("----> "+ getAID().getLocalName() + " Baking for " + bakingTime);
            this.productPerSlot = findProductPerSlot();
            System.out.println("Each slot can hold " + productPerSlot + " " + productType);
            System.out.println("=========================================" );
        }

        public int findProductPerSlot(){
            // Get the Number of products the slot can fit based on product type
            for (ProductMas product : products) {
                if (product.getGuid().equals(productType)){
                    Batch batch = product.getBatch();
                    return batch.getBreadsPerOven();
                }
            }
            return (int) 0;
        }

        // public void findAvailableSlot(int slotsNeeded){
        //     // Find the required amount of slots that available
        //     int slotCounter = 0;
        //     for (Oven oven : ovens){
        //         for (OvenSlot slot : oven.getOvenSlots()){
        //             if (slotCounter < slotsNeeded){
        //                 if (slot.isAvailable()){
        //                     // System.out.println("Slot " + slotCounter + " is available");
        //                     chosenSlots.add(slot);
        //                     slot.setAvailable(false);
        //                     slotCounter++;
        //                 }
        //             }else{
        //                 return;
        //             }
        //         }
        //     }
        // }
        //

        //
        // // public int[] findOvenRates(String ovenGuid){
        // //     // Reads the heating and cooling rate from the oven using
        // //     int rates[] = new int[2];
        // //     for (Oven oven : ovens){
        // //         if (oven.getGuid().equals(ovenGuid)){
        // //             rates[0] = oven.getHeatingRate();
        // //             rates[1] = oven.getCoolingRate();
        // //             return rates;
        // //         }
        // //     }
        // //     return rates;
        // // }
        //
        // public boolean readyToBake(boolean[] slotsReady){
        //     // Read the values of the boolean array. This is used
        //     // to heat all the chosenSlots at the same time.
        //     for (boolean slotFlag : slotsReady){
        //         if (!slotFlag){
        //             return false;
        //         }
        //     }
        //     return true;
        // }
        //
        // public void releaseUsedSlots(){
        //     // Set the chosenSlots as available and delete the array
        //     for (OvenSlot slot : chosenSlots) {
        //         slot.setAvailable(true);
        //
        //     }
        //     chosenSlots = new Vector<OvenSlot> ();
        // }

        public void action(){
            // Iterate over the different orders
            if (productIdx < productQuantities.size()){
                if (!needHeating.get() && !needCooling.get() && !bakingInProcess.get()){// Initial case

                    System.out.println("-----> Processing order " + productIdx);
                    requestedQuantity = productQuantities.get(productIdx);
                    System.out.println("-----> I need to bake " + requestedQuantity + " " + productType);
                    int slotsNeeded = (int) Math.ceil( ((float)requestedQuantity/(float)productPerSlot));
                    System.out.println("-----> I need " + slotsNeeded + " slots");
                    // findAvailableSlot(slotsNeeded);
                    // System.out.println("-----> Found " + slotsNeeded + " slots");
                    slotsReady = new boolean [slotsNeeded];
                }
            }
            //     if (slotIdx < chosenSlots.size() && !bakingInProcess.get()){
            //         // Iterate over each slot
            //         // System.out.println("-----> Checking slot " + slotIdx);
            //         OvenSlot slot = chosenSlots.get(slotIdx);
            //         // int rates[] = findOvenRates(slot.getOvenGuid());
            //         // int ovenHeatingRate = rates[0];
            //         // int ovenCoolingRate = rates[1];
            //
            //         // System.out.println("-----> Oven rates " + ovenHeatingRate + " - " + ovenCoolingRate);
            //         // System.out.println("-----> Slot  currentTemp " + slot.getCurrentTemp() );
            //
            //         // Checking if needing to heat up
            //         if ((slot.getCurrentTemp() < (float) bakingTemp) &&
            //            (Math.abs(slot.getCurrentTemp() - (float) bakingTemp) > (float) ovenHeatingRate)){
            //             needHeating.set(true);
            //             needCooling.set(false);
            //             // System.out.println("I need to heat up" );
            //
            //             if (heatingUp.get()){
            //                 heatingUp.set(false);
            //                 slot.setCurrentTemp( slot.getCurrentTemp() + (float) ovenHeatingRate);
            //                 System.out.println("=========================================" );
            //                 System.out.println("Heating up slot " + slotIdx);
            //                 System.out.println("Current temperature is " + slot.getCurrentTemp() + " desired is " + bakingTemp);
            //                 System.out.println("=========================================" );
            //             }
            //         }
            //         else{
            //             // Checking if needing to cool down
            //             if ((slot.getCurrentTemp() > (float) bakingTemp) &&
            //                  (Math.abs(slot.getCurrentTemp() - (float) bakingTemp) > (float) ovenCoolingRate)){
            //                 needCooling.set(true);
            //                 needHeating.set(false);
            //                 System.out.println("=========================================" );
            //                 System.out.println("I need to cool down" );
            //
            //                 if (coolingDown.get()){
            //                     coolingDown.set(false);
            //                     System.out.println("Cooling down "  + slotIdx + 1);
            //                     System.out.println("Current temperature is " + slot.getCurrentTemp());
            //                     System.out.println("=========================================" );
            //                     slot.setCurrentTemp( slot.getCurrentTemp() - (float) ovenCoolingRate);
            //                 }
            //             }
            //             else{ // Ideal temperature reached
            //                 System.out.println("I am ready to bake" );
            //                 needCooling.set(false);
            //                 needHeating.set(false);
            //                 slotsReady[slotIdx] = true;
            //             }
            //         }
            //
            //         if (readyToBake(slotsReady)){
            //             // All the slots are ready to bake
            //             bakingInProcess.set(true);
            //         }
            //         else{
            //             slotIdx++;
            //         }
            //     }
            //     else{
            //         if (!bakingInProcess.get()){
            //             // slots are not ready to bake. Iterate again
            //             slotIdx = 0;
            //         }
            //         else{
            //             if (bakingCounter.get() >= bakingTime){
            //                 // We have finished baking this order
            //                 bakingInProcess.set(false);
            //                 bakingCounter.set(0);
            //                 productIdx++;
            //             }
            //         }
            //     }
            // }
            // else{
            //     // We have baked all the orders
            //     bakedFullOrder.set(true);
            //     System.out.println("=========================================" );
            //     System.out.println("I have finished baking" );
            //     System.out.println("=========================================" );
            //     addBehaviour(new SendBakingNotification());
            // }

        }
        public boolean done(){
            if (!bakedFullOrder.get()){
                return false;
            }else{
                return true;
            }
        }
    }

    // Send a bakingNotification msg to the bakingManager agents
    // private class SendBakingNotification extends Behaviour {
    //     private MessageTemplate mt;
    //     private int option = 0;
    //     private Gson gson = new Gson();
    //     private BakingNotification bakingNotification = new BakingNotification(guids,productType,productQuantities);
    //     private String bakingNotificationString = gson.toJson(bakingNotification);
    //
    //     public void action() {
    //         switch (option) {
    //             case 0:
    //
    //                 ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    //                 System.out.println("-----> Baking notification string " + bakingNotificationString);
    //                 msg.setContent(bakingNotificationString);
    //                 msg.setConversationId("baking-notification");
    //
    //                 // Send bakingNotification msg to bakingManagerAgents
    //                 // for (int i = 0; i < bakingManagerAgents.length; i++){
    //                 //     msg.addReceiver(bakingManagerAgents[i]);
    //                 // }
    //
    //                 baseAgent.sendMessage(msg);
    //
    //                 option = 1;
    //                 System.out.println(getAID().getLocalName() + " Sent bakingNotification");
    //                 break;
    //
    //             case 1:
    //                 mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
    //                     MessageTemplate.MatchConversationId("baking-notification-reply"));
    //                 ACLMessage reply = baseAgent.receive(mt);
    //
    //                 if (reply != null) {
    //                     System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
    //                     option = 2;
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
