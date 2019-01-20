package org.mas_maas.agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
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
import org.maas.Objects.WorkQueue;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.maas.agents.BaseAgent;

public class OvenAgent extends BaseAgent {
    private AID bakingManagerAgent;

    private AtomicBoolean bakingAllowed = new AtomicBoolean(false);
    private AtomicBoolean heatingAllowed = new AtomicBoolean(false);
    private AtomicBoolean coolingAllowed = new AtomicBoolean(false);
    private AtomicInteger messageProcessing = new AtomicInteger(0);
    private AtomicBoolean isInProductionTime = new AtomicBoolean (false);

    // private Oven oven;
    private Vector<Oven> ovens = new Vector<Oven> ();
    private Vector<Equipment> equipment;
    private Vector<OvenSlot> bookedSlots = new Vector<OvenSlot> ();
    private Vector<OvenSlot> slots = new Vector<OvenSlot> ();

    private Bakery bakery;
    private String bakeryId;

    private String ovenName;
    private String bakingManagerName;
    private String scenarioPath;

    // private int bakingTemp;
    // private String productType;
    // private Vector<String> guids;
    // private Vector<Integer> productQuantities;

    private Vector<ProductMas> products;
    private ArrayList<BakingRequest> bakingRequests = new ArrayList<BakingRequest>();

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

        initializeSlots();

        // Time tracker behavior
        addBehaviour(new timeTracker());
        //addBehaviour(new ReceiveProposalRequests());
        // Creating receive bakingRequests behaviour
        addBehaviour(new ReceiveBakingRequests());

        addBehaviour(new checkingBakingRequests());

        // addBehaviour(new Baking());
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

            if (messageProcessing.get() <= 0)
            {
                // Production time is from midnight to lunch (from 00.00 hrs to 12 hrs)
               if ((baseAgent.getCurrentTime().greaterThan(new Time(baseAgent.getCurrentDay(), 0, 0)) ||

                       baseAgent.getCurrentTime().equals(new Time(baseAgent.getCurrentDay(), 0, 0))) &&

                       baseAgent.getCurrentTime().lessThan(new Time(baseAgent.getCurrentDay(), 12, 0)))
                {
                   // bakingAllowed.set(true);
                   // heatingAllowed.set(true);
                   // coolingAllowed.set(true);
                   isInProductionTime.set(true);
                   addBehaviour(new Baking());
               }else{
                   isInProductionTime.set(false);
               }
                baseAgent.finished();
            }
        }
    }

    // Behaviour that checks the needsKneading workqueue and activates CFP for requesting kneading
    private class checkingBakingRequests extends CyclicBehaviour{ //TODO:
        public void action(){
            messageProcessing.incrementAndGet();

            if (bakingRequests.size() > 0 && isInProductionTime.get()){
                BakingRequest bakingRequest = bakingRequests.get(0);
                bakingRequests.remove(0);

                int bakingTemp = bakingRequest.getBakingTemp();
                float bakingTime = bakingRequest.getBakingTime();
                String productType = bakingRequest.getProductType();
                Vector<String> guids = bakingRequest.getGuids();
                Vector<Integer> productQuantities = bakingRequest.getProductQuantities();
                Vector<Integer> slotsNeeded = bakingRequest.getSlotsNeeded();
                int productPerSlot = bakingRequest.getProductPerSlot();

                 int guidsPending = 0;

                for (int i = 0; i < slotsNeeded.size(); i++){

                    // Book the slots for an order guid. Update the number of slots needed. If all slots were booked
                    // the updated number of slots needed is zero.
                    if (slotsNeeded.get(i) > 0){

                        int values[] = bookSlots(slotsNeeded.get(i), productType, bakingTemp, bakingTime,
                                        productQuantities.get(i), productPerSlot, guids.get(i));
                        slotsNeeded.set(i, values[0]);
                        productQuantities.set(i, values[1]);

                        if (slotsNeeded.get(i) != 0){
                            guidsPending ++;
                        }
                    }
                }


                // If not all the slots for the baking request were book, readd the baking request to the queue

                if (guidsPending > 0){
                    bakingRequest.setSlotsNeeded(slotsNeeded);
                    bakingRequest.setProductQuantities(productQuantities);
                    // System.out.println(" Readding baking request to the bakingRequests list " + bakingRequest);

                    bakingRequests.add(bakingRequest);
                }
            }
            messageProcessing.decrementAndGet();

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
        // System.out.println("Bakery name " + bakery.getName());

        for (int i = 0; i < equipment.size(); i++){
            if (equipment.get(i) instanceof Oven){
                // System.out.println("Kneading machines found " + equipment.get(i));
                ovens.add((Oven) equipment.get(i));
            }
        }
        // System.out.println("=========================================" );
        // System.out.println(getAID().getLocalName() + " Ovens found " + ovens.size());
        // System.out.println("=========================================" );
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

    public void initializeSlots(){
        for (Oven oven : ovens){
            for (OvenSlot slot : oven.getOvenSlots()){
                slots.add(slot);
            }
        }
        // System.out.println("----> Total number of slots " + slots.size());
        // System.out.println(slots);
    }

    public int[] bookSlots(int slotsNeeded, String productType, int bakingTemp, float bakingTime, Integer quantity, int productPerSlot, String guid){
        // Find a specific amount of slots available
        // TODO: What happends if there are not enough?
        // System.out.println(" Slots needed " + slotsNeeded);
        // System.out.println(" Slots available " + slots.size());
        // System.out.println("Using: ");
        int slotsBooked = 0;
        // int slotsNotAvailable = 0;
        int rates[] = new int[2];
        int values[] = new int[2];
        // Vector<OvenSlot> tmp_slots = new Vector<OvenSlot>(slots);

        for (int slotIdx = 0; slotIdx < slots.size(); slotIdx++){
            // OvenSlot slot = (OvenSlot) slots.get(slotIdx);
            // System.out.println("i: " + slotIdx);
            if (slotsBooked < slotsNeeded){
                if (slots.get(slotIdx).isAvailable()){
                    // // System.out.println("Slot of " + slots.get(slotIdx).getOvenGuid());
                    slots.get(slotIdx).setAvailable(false);
                    slots.get(slotIdx).setProductType(productType);
                    rates = findOvenRates(slots.get(slotIdx).getOvenGuid());
                    slots.get(slotIdx).setHeatingRate(rates[0]);
                    slots.get(slotIdx).setCoolingRate(rates[1]);
                    // slots.get(slotIdx).setReadyToBake(slots.get(slotIdx).getCurrentTemp() == bakingTemp);
                    slots.get(slotIdx).setBakingTime(bakingTime);
                    slots.get(slotIdx).setBakingTemp(bakingTemp);

                    if (quantity < productPerSlot){
                        slots.get(slotIdx).setQuantity(quantity);
                        quantity = 0;
                    }else if ((quantity - productPerSlot) >= 0){
                        slots.get(slotIdx).setQuantity(productPerSlot);
                        quantity = quantity - productPerSlot;
                    }

                    // if ((quantity - productPerSlot) >= 0){
                    //     slots.get(slotIdx).setQuantity(productPerSlot);
                    // }else{
                    //     slots.get(slotIdx).setQuantity(Math.abs(quantity - productPerSlot));
                    // }
                    slots.get(slotIdx).setGuid(guid);
                    //
                    // tmp_slots.remove(slotIdx);
                    // bookedSlots.add(slot);
                    //
                    // System.out.println("Slot of " + slots.get(slotIdx).getOvenGuid() +" will bake " + slots.get(slotIdx).getQuantity() + " "
                    //         + productType + " for " + bakingTime + " for "+ slots.get(slotIdx).getGuid());

                    slotsBooked++;
                    // System.out.println("c: " + slotsBooked);
                    // quantity = quantity - productPerSlot;
                }else{
                    // slotsNotAvailable++;
                }
            }else{
                values[0] = (int) 0; //slotsNeeded
                values[1] = quantity;
                // System.out.println("* Total number of slots booked: " + bookedSlots.size());
                // System.out.println(" Total number of slots remaining: " + (slots.size() - slotsBooked));
                return values;
            }

        }
        // slots = new Vector<OvenSlot>(tmp_slots);
        values[0] = (int) (slotsNeeded - slotsBooked); //slotsNeeded
        values[1] = quantity;
        // System.out.println("Total number of slots booked: " + bookedSlots.size());
        // System.out.println("Total number of slots remaining: " + (slots.size() - slotsBooked));
        return values;
    }

    // Receiving Baking requests behavior
    private class ReceiveBakingRequests extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("baking-request"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {

            	String content = msg.getContent();

                // System.out.println("================================================================================");
                // System.out.println(getAID().getLocalName()+" received baking requests from \n \t" + msg.getSender()
                //     + " for: \n \t" + content);
                // System.out.println("================================================================================");

                BakingRequest bakingRequest = JSONConverter.parseBakingRequest(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Baking request was received");
                reply.setConversationId("baking-request-reply");
                baseAgent.sendMessage(reply);

                bakingRequests.add(bakingRequest);

                // bakingTemp = bakingRequest.getBakingTemp();
                // Float bakingTime = bakingRequest.getBakingTime();
                // productType = bakingRequest.getProductType();
                // guids = bakingRequest.getGuids();
                // productQuantities = bakingRequest.getProductQuantities();

                // addBehaviour(new Baking(bakingTime));
                messageProcessing.decrementAndGet();

            }

            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
}

    private class Baking extends OneShotBehaviour {

        public void releaseSlot(int slotIdx){
            // OvenSlot slot = (OvenSlot) bookedSlots.get(slotIdx);
            // System.out.println("Releasing slot " + slots.get(slotIdx).getOvenGuid());
            slots.get(slotIdx).setAvailable(true);
            slots.get(slotIdx).setProductType(null);
            slots.get(slotIdx).setHeatingRate(0);
            slots.get(slotIdx).setCoolingRate(0);
            // slots.get(slotIdx).setReadyToBake(false);
            slots.get(slotIdx).setBakingTime(0);
            slots.get(slotIdx).setBakingTemp(0);
            slots.get(slotIdx).setBakingCounter(0);
            slots.get(slotIdx).setQuantity(0);
            slots.get(slotIdx).setGuid(null);
            // bookedSlots.remove(slotIdx);
            // slots.add(slot);
        }

        public void action(){

            messageProcessing.getAndIncrement();
            for (int slotIdx = 0; slotIdx < slots.size(); slotIdx++){
                // OvenSlot slot = (OvenSlot) slots.get(slotIdx);

                if (!slots.get(slotIdx).isAvailable()){
                    // Needs Heating
                    if ((slots.get(slotIdx).getCurrentTemp() < (float) slots.get(slotIdx).getBakingTemp()) &&
                        (Math.abs(slots.get(slotIdx).getCurrentTemp() - (float) slots.get(slotIdx).getBakingTemp()) > (float) slots.get(slotIdx).getHeatingRate())){

                        // if (heatingAllowed.get()){
                            // heatingAllowed.set(false);
                            slots.get(slotIdx).setCurrentTemp( slots.get(slotIdx).getCurrentTemp() + (float) slots.get(slotIdx).getHeatingRate());
                            // System.out.println("=========================================" );
                            // System.out.println("Heating up slot of " + slots.get(slotIdx).getOvenGuid() + " with current temperature of "
                            //     + slots.get(slotIdx).getCurrentTemp() + ". Desired temperature  is " + slots.get(slotIdx).getBakingTemp());
                            // System.out.println("=========================================" );
                        // }
                    }
                    // Needs Cooling
                    else if ((slots.get(slotIdx).getCurrentTemp() > (float) slots.get(slotIdx).getBakingTemp()) &&
                            (Math.abs(slots.get(slotIdx).getCurrentTemp() - (float) slots.get(slotIdx).getBakingTemp()) > (float) slots.get(slotIdx).getCoolingRate())){
                            slots.get(slotIdx).setCurrentTemp( slots.get(slotIdx).getCurrentTemp() - (float) slots.get(slotIdx).getHeatingRate());

                            // if (coolingAllowed.get()){
                                // coolingAllowed.set(false);
                                // System.out.println("=========================================" );
                                // System.out.println("Cooling down slot of " + slots.get(slotIdx).getOvenGuid() + " with current temperature of "
                                //     + slots.get(slotIdx).getCurrentTemp() + ". Desired temperature  is " + slots.get(slotIdx).getBakingTemp());
                                // System.out.println("=========================================" );
                            // }
                    }
                    // Desired temperature is reached
                    else{
                        if (slots.get(slotIdx).getBakingTime() >= slots.get(slotIdx).getBakingCounter()){
                            // Slot has finished baking
                            System.out.println(" Slot of " + slots.get(slotIdx).getOvenGuid() + " finished baking "
                            + slots.get(slotIdx).getQuantity() + " " + slots.get(slotIdx).getProductType() + " for " + slots.get(slotIdx).getGuid());

                            Vector<Integer> quantity = new Vector<Integer>();;
                            quantity.add(slots.get(slotIdx).getQuantity());

                            Vector<String> guids_done = new Vector<String> ();
                            guids_done.add(slots.get(slotIdx).getGuid());

                            Gson gson = new Gson();
                            BakingNotification bakingNotification = new BakingNotification(guids_done,
                                    slots.get(slotIdx).getProductType(),quantity);

                            String bakingNotificationString = gson.toJson(bakingNotification);

                            releaseSlot(slotIdx);
                            messageProcessing.decrementAndGet();
                            addBehaviour(new SendBakingNotification(bakingNotificationString));

                        }else{
                             // if (bakingAllowed.get()){
                            // bakingAllowed.set(false);
                            slots.get(slotIdx).setBakingCounter(slots.get(slotIdx).getBakingCounter() + 1);
                            // System.out.println("=========================================" );
                            System.out.println(">>>>> Baking counter of slot of " + slots.get(slotIdx).getOvenGuid()
                                + " increased to " + slots.get(slotIdx).getBakingCounter() +
                                " for " + slots.get(slotIdx).getGuid() + " for product " + slots.get(slotIdx).getProductType() +" <<<<<");
                            // System.out.println("=========================================" );
                        }


                    }
                }
            }
            messageProcessing.decrementAndGet();
        }
    }

//    Send a bakingNotification msg to the bakingManager agents
    private class SendBakingNotification extends Behaviour {
        private MessageTemplate mt;
        private int option = 0;
        private String bakingNotificationString;

        private SendBakingNotification(String bakingNotificationString){
            this.bakingNotificationString = bakingNotificationString;
        }

        public void action() {
            messageProcessing.getAndIncrement();
            switch (option) {
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(bakingNotificationString);
                    msg.setConversationId("baking-notification");
                    msg.addReceiver(bakingManagerAgent);

                    baseAgent.sendMessage(msg);

                    // System.out.println(getAID().getLocalName() + " Sent bakingNotification " + bakingNotificationString);

                    option = 1;
                    break;

                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("baking-notification-reply"));

                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {
                        // System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                        option = 2;
                    }
                    else {
                        messageProcessing.getAndDecrement();
                        block();
                    }
                    messageProcessing.getAndDecrement();
                    break;

                default:
                    messageProcessing.getAndDecrement();
                    break;
            }
        }

        public boolean done() {
            return option == 2;
       }
    }

}
