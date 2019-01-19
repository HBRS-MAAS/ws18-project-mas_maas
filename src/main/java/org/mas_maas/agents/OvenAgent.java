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

    // private Oven oven;
    private Vector<Oven> ovens = new Vector<Oven> ();
    private Vector<Equipment> equipment;

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
            this.ovenName = (String) args[0];
            this.bakingManagerName = (String) args[1];
            this.scenarioPath = (String) args[2];
            this.bakeryId = (String) args[3];
        }

        //Read the scenario file and get the bakery with this.bakeryId
        getBakery(scenarioPath);

        this.getBakingManagerAID();

        System.out.println("Hello! " + getAID().getLocalName() + " is ready." + "its BakingManager is: " +
            bakingManagerName);

        this.register(this.ovenName, "JADE-bakery");

        //oven.setAvailable(true);
        getOvens();

        // Time tracker behavior
        addBehaviour(new timeTracker());
        //addBehaviour(new ReceiveProposalRequests());
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

            if (bakingInProcess.get() && isInProductionTime.get()){
                int curBakingCount = bakingCounter.incrementAndGet();
                System.out.println("=========================================" );
                // System.out.println(">>>>> Oven Clock-> " + baseAgent.getCurrentHour() + " <<<<<");
                System.out.println(">>>>> baking Counter -> " + curBakingCount +
                                " \n for " + getAID().getLocalName() + " <<<<<");
                System.out.println("=========================================" );
            }

            else{
                if (needHeating.get() && isInProductionTime.get()){
                    // System.out.println("------->  Allowing heat");
                    heatingUp.set(true);
                }
                if (needCooling.get() && isInProductionTime.get()){
                    // System.out.println("-------> Winter is coming");
                    coolingDown.set(true);
                }

            }

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

    public void getOvens(){
        equipment = bakery.getEquipment();
        System.out.println("Bakery name " + bakery.getName());

        for (int i = 0; i < equipment.size(); i++){
            if (equipment.get(i) instanceof Oven){
                // System.out.println("Kneading machines found " + equipment.get(i));
                ovens.add((Oven) equipment.get(i));
            }
        }
        System.out.println("=========================================" );
        System.out.println(getAID().getLocalName() + " Ovens found " + ovens.size());
        System.out.println("=========================================" );
    }

    // Receiving Baking requests behavior
    private class ReceiveBakingRequests extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("baking-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

            	String content = msg.getContent();

                System.out.println("================================================================================");
                System.out.println(getAID().getLocalName()+" received baking requests from \n \t" + msg.getSender()
                    + " for: \n \t" + content);
                System.out.println("================================================================================");

                BakingRequest bakingRequest = JSONConverter.parseBakingRequest(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Baking request was received");
                reply.setConversationId("baking-request-reply");
                baseAgent.sendMessage(reply);

                bakingTemp = bakingRequest.getBakingTemp();
                Float bakingTime = bakingRequest.getBakingTime();
                productType = bakingRequest.getProductType();
                guids = bakingRequest.getGuids();
                productQuantities = bakingRequest.getProductQuantities();

                addBehaviour(new Baking(bakingTime));
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

        public void findAvailableSlot(int slotsNeeded){
            // Find a specific amount of slots available
            // TODO: What happends if there are not enough?
            System.out.println("Using: ");
            int slotCounter = 0;
            for (Oven oven : ovens){
                for (OvenSlot slot : oven.getOvenSlots()){
                    if (slotCounter < slotsNeeded){
                        if (slot.isAvailable()){
                            System.out.println("Slot of " + slot.getOvenGuid());
                            chosenSlots.add(slot);
                            slot.setAvailable(false);
                            slotCounter++;
                        }
                    }else{
                        return;
                    }
                }
            }
        }



        public int[] findOvenRates(String ovenGuid){
            // Reads the heating and cooling rate from the oven using
            int rates[] = new int[2];
            for (Oven oven : ovens){
                if (oven.getGuid().equals(ovenGuid)){
                    rates[0] = oven.getHeatingRate();
                    rates[1] = oven.getCoolingRate();
                    return rates;
                }
            }
            return rates;
        }

        public boolean readyToBake(boolean[] slotsReady){
            // Read the values of the boolean array. This is used
            // to heat all the chosenSlots at the same time.
            for (boolean slotFlag : slotsReady){
                if (!slotFlag){
                    return false;
                }
            }
            return true;
        }
        //
        public void releaseUsedSlots(){
            // Set the chosenSlots as available and delete the array
            for (OvenSlot slot : chosenSlots) {
                slot.setAvailable(true);

            }
            chosenSlots = new Vector<OvenSlot> ();
        }

        public void action(){
            // Iterate over the different orders
            if (productIdx < productQuantities.size()){
                // Initial case
                if (!bakingInProcess.get() && chosenSlots.isEmpty()){

                    System.out.println("-----> Processing order " + guids.get(productIdx));
                    requestedQuantity = productQuantities.get(productIdx);
                    System.out.println("-----> I need to bake " + requestedQuantity + " " + productType);
                    int slotsNeeded = (int) Math.ceil( ((float)requestedQuantity/(float)productPerSlot));
                    System.out.println("-----> I need " + slotsNeeded + " slots");
                    findAvailableSlot(slotsNeeded);
                    // System.out.println("-----> Found " + slotsNeeded + " slots");
                    slotsReady = new boolean [slotsNeeded];
                }
                // Iterate over each slot
                if (slotIdx < chosenSlots.size() && !bakingInProcess.get()){

                    // System.out.println("-----> Checking slot " + slotIdx);
                    if (slotsReady[slotIdx] == false){
                        OvenSlot slot = chosenSlots.get(slotIdx);
                        int rates[] = findOvenRates(slot.getOvenGuid());
                        int ovenHeatingRate = rates[0];
                        int ovenCoolingRate = rates[1];

                        // System.out.println("-----> Oven rates: Heating " + ovenHeatingRate + " - Cooling " + ovenCoolingRate);
                        // System.out.println("-----> Slot  " + slotIdx + "currentTemp " + slot.getCurrentTemp() );

                        // Checking if needing to heat up
                        if ((slot.getCurrentTemp() < (float) bakingTemp) &&
                            (Math.abs(slot.getCurrentTemp() - (float) bakingTemp) > (float) ovenHeatingRate)){
                                needHeating.set(true);
                                needCooling.set(false);
                                // System.out.println("I need to heat up" );

                                if (heatingUp.get()){
                                    heatingUp.set(false);
                                    slot.setCurrentTemp( slot.getCurrentTemp() + (float) ovenHeatingRate);
                                    System.out.println("=========================================" );
                                    System.out.println("Heating up slot " + slotIdx +" of " + slot.getOvenGuid());
                                    System.out.println("Current temperature of slot " + slotIdx +" of " + slot.getOvenGuid()
                                            +" is " + slot.getCurrentTemp() + " desired is " + bakingTemp);
                                    System.out.println("=========================================" );
                                }
                        }
                        else if ((slot.getCurrentTemp() > (float) bakingTemp) &&
                                (Math.abs(slot.getCurrentTemp() - (float) bakingTemp) > (float) ovenCoolingRate)){
                                    needCooling.set(true);
                                    needHeating.set(false);
                                    // System.out.println("I need to cool down" );
            //
                                if (coolingDown.get()){
                                    coolingDown.set(false);
                                    System.out.println("=========================================" );
                                    System.out.println("Cooling down "  + slotIdx + " of oven " + slot.getOvenGuid());
                                    System.out.println("Current temperature of slot " + slotIdx +" of oven " + slot.getOvenGuid()
                                            +" is " + slot.getCurrentTemp() + " desired is " + bakingTemp);
                                    System.out.println("=========================================" );
                                    slot.setCurrentTemp( slot.getCurrentTemp() - (float) ovenCoolingRate);
                                }
                        }
                        else{ // Ideal temperature reached
                            System.out.println("Slot " + slotIdx +" of " + slot.getOvenGuid()
                                +" \n \t has reached the desired temperature to bake " + productType + " for order "+ guids.get(productIdx) );
                            needCooling.set(false);
                            needHeating.set(false);
                            slotsReady[slotIdx] = true;

                            if (readyToBake(slotsReady)){
                                // All the slots are ready to bake
                                System.out.println("All the slots needed are ready to bake " + productType
                                    + " for order "+ guids.get(productIdx) );
                                bakingInProcess.set(true);
                            }
                        }
                    }
                    // System.out.println("Iterating to a new slot");
                    slotIdx++;
                }
                else{
                    if (!bakingInProcess.get()){
                        // slots are not ready to bake. Iterate again
                        slotIdx = 0;
                    }
                    else{
                        if (bakingCounter.get() >= bakingTime){
                            // We have finished baking this order
                            System.out.println("I have finished baking " + guids.get(productIdx) );
                            bakingInProcess.set(false);
                            bakingCounter.set(0);
                            releaseUsedSlots();
                            productIdx++; //TODO: Change this implementation to not wait for an order to go to another one
                        }
                    }
                }
            }
            else{
                // We have baked all the orders
                bakedFullOrder.set(true);
                System.out.println("=========================================" );
                System.out.println("I have finished baking all the orders in the bakingRequest" );
                System.out.println("=========================================" );
                // addBehaviour(new SendBakingNotification());
            }

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
