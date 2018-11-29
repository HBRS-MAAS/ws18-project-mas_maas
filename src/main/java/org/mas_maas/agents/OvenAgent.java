package org.mas_maas.agents;

import java.util.Vector;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.*;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.BakingNotification;
import org.mas_maas.messages.BakingRequest;

import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.Oven;
import org.mas_maas.objects.OvenSlot;
import org.mas_maas.objects.Equipment;
import org.mas_maas.objects.Product;
import org.mas_maas.objects.Batch;

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

public class OvenAgent extends BaseAgent {
    private AID [] bakingManagerAgents;

    private AtomicBoolean bakingInProcess = new AtomicBoolean(false);
    private AtomicBoolean heatingUp = new AtomicBoolean(false);
    private AtomicBoolean coolingDown = new AtomicBoolean(false);
    private AtomicBoolean bakedFullOrder = new AtomicBoolean(false);
    private AtomicBoolean needHeating = new AtomicBoolean(false);
    private AtomicBoolean needCooling = new AtomicBoolean(false);

    private Bakery bakery;
    private Vector<Oven> ovens = new Vector<Oven> ();
    private Vector<Equipment> equipment;

    private int bakingTemp;
    private String productType;
    private Vector<String> guids;
    private Vector<Integer> productQuantities;

    private Vector<Product> products;

    private int bakingCounter;

    protected void setup() {
        super.setup();

        System.out.println(getAID().getLocalName() + " is ready.");

        // Register BakingMachine Agent to the yellow Pages
        this.register("OvenAgent", "JADE-bakery");

        // Get Agents AIDS
        this.getBakingManagerAIDs();

        // Load bakery information (includes recipes for each product)
        getbakery();

        // Get ovens from bakery
        this.getOvens();

        bakingCounter = 0;
        // Time tracker behavior
        addBehaviour(new timeTracker());

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceiveBakingRequests());

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

    public void getbakery(){

        String jsonDir = "src/main/resources/config/shared_stage_communication/";
        try {
            // System.out.println("Working Directory = " + System.getProperty("user.dir"));
            String bakeryFile = new Scanner(new File(jsonDir + "bakery.json")).useDelimiter("\\Z").next();
            Vector<Bakery> bakeries = JSONConverter.parseBakeries(bakeryFile);
            for (Bakery bakery : bakeries)
            {
                this.bakery = bakery;
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
        System.out.println("Ovens found " + ovens.size());
        System.out.println("=========================================" );
    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }else{
                if (bakingInProcess.get()){
                    bakingCounter++;
                    System.out.println("=========================================" );
                    System.out.println("-------> Oven Clock-> " + baseAgent.getCurrentHour());
                    System.out.println("-------> baking Counter -> " + bakingCounter);
                    System.out.println("=========================================" );
                }else{
                    if (needHeating.get()){
                        // System.out.println("------->  Allowing heat");
                        heatingUp.set(true);
                    }
                    if (needCooling.get()){
                        // System.out.println("-------> Winter is coming");
                        coolingDown.set(true);
                    }

                }
            }
            baseAgent.finished();
        }
    }

    // Receiving Baking requests behavior
    private class ReceiveBakingRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("baking-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                System.out.println("----> " + getAID().getLocalName() + " received baking requests from " + msg.getSender());
                String content = msg.getContent();
                System.out.println("Baking request contains -> " + content);
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

            }

            else {
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
            System.out.println("Each slot can holds " + productPerSlot + " " + productType);
            System.out.println("=========================================" );
        }

        public void findAvailableSlot(int slotsNeeded){
            // Find the required amount of slots that available
            int slotCounter = 0;
            for (Oven oven : ovens){
                for (OvenSlot slot : oven.getOvenSlots()){
                    if (slotCounter < slotsNeeded){
                        if (slot.isAvailable()){
                            // System.out.println("Slot " + slotCounter + " is available");
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

        public int findProductPerSlot(){
            // Get the Number of products the slot can fit based on product type
            for (Product product : products) {
				if (product.getGuid().equals(productType)){
                    Batch batch = product.getBatch();
                    return batch.getBreadsPerOven();
                }
			}
            return (int) 0;
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
                if (!needHeating.get() && !needHeating.get() && !bakingInProcess.get()){
                    // Initial case
                    System.out.println("-----> Processing order " + productIdx);
                    requestedQuantity = productQuantities.get(productIdx);
                    System.out.println("-----> I need to bake " + requestedQuantity + " " + productType);
                    int slotsNeeded = (int) Math.ceil( ((float)requestedQuantity/(float)productPerSlot));
                    System.out.println("-----> I need " + slotsNeeded + " slots");
                    findAvailableSlot(slotsNeeded);
                    // System.out.println("-----> Found " + slotsNeeded + " slots");
                    slotsReady = new boolean [slotsNeeded];
                }

                if (slotIdx < chosenSlots.size() && !bakingInProcess.get()){
                    // Iterate over each slot
                    // System.out.println("-----> Checking slot " + slotIdx);
                    OvenSlot slot = chosenSlots.get(slotIdx);
                    int rates[] = findOvenRates(slot.getOvenGuid());
                    int ovenHeatingRate = rates[0];
                    int ovenCoolingRate = rates[1];

                    // System.out.println("-----> Oven rates " + ovenHeatingRate + " - " + ovenCoolingRate);
                    // System.out.println("-----> Slot  currentTemp " + slot.getCurrentTemp() );

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
                            System.out.println("Heating up slot " + slotIdx);
                            System.out.println("Current temperature is " + slot.getCurrentTemp() + " desired is " + bakingTemp);
                            System.out.println("=========================================" );
                        }
                    }
                    else{
                        // Checking if needing to cool down
                        if ((slot.getCurrentTemp() > (float) bakingTemp) &&
                             (Math.abs(slot.getCurrentTemp() - (float) bakingTemp) > (float) ovenCoolingRate)){
                            needCooling.set(true);
                            needHeating.set(false);
                            System.out.println("=========================================" );
                            System.out.println("I need to cool down" );

                            if (coolingDown.get()){
                                coolingDown.set(false);
                                System.out.println("Cooling down "  + slotIdx + 1);
                                System.out.println("Current temperature is " + slot.getCurrentTemp());
                                System.out.println("=========================================" );
                                slot.setCurrentTemp( slot.getCurrentTemp() - (float) ovenCoolingRate);
                            }
                        }
                        else{ // Ideal temperature reached
                            System.out.println("I am ready to bake" );
                            needCooling.set(false);
                            needHeating.set(false);
                            slotsReady[slotIdx] = true;
                        }
                    }

                    if (readyToBake(slotsReady)){
                        // All the slots are ready to bake
                        bakingInProcess.set(true);
                    }
                    else{
                        slotIdx++;
                    }
                }
                else{
                    if (!bakingInProcess.get()){
                        // slots are not ready to bake. Iterate again
                        slotIdx = 0;
                    }
                    else{
                        if (bakingCounter >= bakingTime){
                            // We have finished baking this order
                            bakingInProcess.set(false);
                            bakingCounter = 0;
                            productIdx++;
                        }
                    }
                }
            }
            else{
                // We have baked all the orders
                bakedFullOrder.set(true);
                System.out.println("=========================================" );
                System.out.println("I have finished baking" );
                System.out.println("=========================================" );
                addBehaviour(new SendBakingNotification());
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
    private class SendBakingNotification extends Behaviour {
        private MessageTemplate mt;
        private int option = 0;
        private Gson gson = new Gson();
        private BakingNotification bakingNotification = new BakingNotification(guids,productType,productQuantities);
        private String bakingNotificationString = gson.toJson(bakingNotification);

        public void action() {
            switch (option) {
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    System.out.println("-----> Baking notification string " + bakingNotificationString);

                    msg.setContent(bakingNotificationString);

                    msg.setConversationId("baking-notification");

                    // Send bakingNotification msg to bakingManagerAgents
                    for (int i = 0; i < bakingManagerAgents.length; i++){
                        msg.addReceiver(bakingManagerAgents[i]);
                    }

                    baseAgent.sendMessage(msg);

                    mt = MessageTemplate.MatchConversationId("baking-notification");

                    System.out.println(getAID().getLocalName() + " Sent bakingNotification");

                    option = 1;


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
