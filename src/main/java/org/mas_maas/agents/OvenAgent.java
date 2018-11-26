package org.mas_maas.agents;

import java.util.Vector;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
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
    private Bakery bakery;
    private Vector<Equipment> ovens = new Vector<Equipment> ();
    private Vector<Equipment> equipment;

    private int bakingTemp;
    private String productType;
    private Vector<String> guids;
    private Vector<Integer> productQuantities;

    private Vector<Product> products;

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
        System.out.println("==============================================");
        System.out.println("Bakery name " + bakery.getName());

        for (int i = 0; i < equipment.size(); i++){
            if (equipment.get(i) instanceof Oven){
                // System.out.println("Kneading machines found " + equipment.get(i));
                ovens.add(equipment.get(i));
            }
        }

        System.out.println("Ovens found " + ovens.size());
        System.out.println("==============================================");

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

                BakingRequest bakingRequest = JSONConverter.parseBakingRequest(content);

                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.CONFIRM);

                reply.setContent("Baking request was received");

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

    // performs Baking process TODO
    private class Baking extends Behaviour {
        private Float bakingTime;
        private Float bakingCounter = (float) 0;
        private int option = 0;
        private int[] indices;
        private int ovenAvailable;
        private int slotAvailable;
        private Vector<OvenSlot> slots;
        private OvenSlot slot_;
        private Oven oven_;

        public Baking(Float bakingTime){
            this.bakingTime = bakingTime;
            System.out.println("----> "+ getAID().getLocalName() + " Baking for " + bakingTime);
        }

        public int[] findAvailableSlot(){
            int indices[] = new int[2];
            for (int oven_idx = 0; oven_idx < ovens.size(); oven_idx++){
                oven_ = (Oven) ovens.get(oven_idx);
                slots = oven_.getOvenSlots();
                // System.out.println("Slots " + slots);
                for (int slot_idx = 0; slot_idx < slots.size(); slot_idx++){
                    if (slots.get(slot_idx).isAvailable() == true){
                        indices[0] = oven_idx;
                        indices[1] = slot_idx;
                        return indices;
                    }

                }
            }
            indices[0] = 99;
            indices[1] = 99;
            return indices;
        }

        public int findProductPerSlot(){
            for (Product product : products) {
				if (product.getGuid().equals(productType)){
                    Batch batch = product.getBatch();
                    return batch.getBreadsPerOven();
                }
			}
            return (int) 0;
        }

        public void action(){

            indices = findAvailableSlot();
            ovenAvailable = indices[0];
            slotAvailable = indices[1];

            int productPerSlot =  findProductPerSlot();

            System.out.println("Baking " + productQuantities + productType);
            System.out.println("Breads per slot " + productPerSlot);

            System.out.format("Oven %d with slot %d is available ", ovenAvailable, slotAvailable);

            if (getAllowAction() && ovenAvailable != 99 && slotAvailable != 99){
                oven_ = (Oven) ovens.get(ovenAvailable);
                int ovenHeatingRate = oven_.getHeatingRate();
                int ovenCoolingRate = oven_.getCoolingRate();
                slot_ = oven_.getOvenSlots().get(slotAvailable);

                slot_.setAvailable(false);

                System.out.println(" ----> Baking Temp " + bakingTemp);
                System.out.println(" ----> Oven heating rate " + ovenHeatingRate);
                System.out.println(" ----> Oven cooling rate " + ovenCoolingRate);
                //
                // System.out.println(" ---->  " + Math.abs(slot_.getCurrentTemp() - (float) bakingTemp));

                while ((slot_.getCurrentTemp() < (float) bakingTemp) &&
                       (Math.abs(slot_.getCurrentTemp() - (float) bakingTemp) > (float) ovenHeatingRate)){
                    System.out.println("Current temperature is " + slot_.getCurrentTemp());
                    System.out.println("Heating up!");
                    slot_.setCurrentTemp( slot_.getCurrentTemp() + (float) ovenHeatingRate);

                }

                while ((slot_.getCurrentTemp() > (float) bakingTemp) &&
                       (Math.abs(slot_.getCurrentTemp() - (float) bakingTemp) > (float) ovenCoolingRate)){
                    System.out.println("Cooling up!");
                    System.out.println("Current temperature is " + slot_.getCurrentTemp());
                    slot_.setCurrentTemp( slot_.getCurrentTemp() - (float) ovenCoolingRate);

                }

                System.out.println("Ready to bake with a temp of " + slot_.getCurrentTemp());

                while(bakingCounter < bakingTime){
                    bakingCounter++;
                    System.out.println("----> " + getAID().getLocalName() + " Baking counter " + bakingCounter);
                }
                addBehaviour(new SendBakingNotification());
                this.done();
            }else{
                System.out.println("No oven slot available");
            }

        }
        public boolean done(){
            baseAgent.finished();
            return true;
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
