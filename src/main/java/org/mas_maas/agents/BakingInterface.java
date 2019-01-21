package org.mas_maas.agents;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import org.maas.JSONConverter;
import org.maas.messages.BakingNotification;
import org.maas.messages.BakingRequest;
import org.maas.messages.CoolingRequest;
import org.maas.messages.DoughNotification;
import org.maas.messages.PreparationNotification;
import org.maas.messages.PreparationRequest;
import org.maas.utils.Time;
import org.maas.Objects.Equipment;
import org.maas.Objects.BakedGood;
import org.maas.Objects.Oven;
import org.maas.Objects.Bakery;
import org.maas.Objects.Batch;
import org.maas.Objects.OrderMas;
import org.maas.Objects.ProductMas;
import org.maas.Objects.ProductStatus;
import org.maas.Objects.Step;
import org.maas.Objects.WorkQueue;

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
import jade.wrapper.*;

import org.maas.agents.BaseAgent;

public class BakingInterface extends BaseAgent {
    private AID dummyOrderProcesser;
    private AID prooferAgent;
    private AID coolingRackAgent;
    private AID bakingPreparationAgent;
    private AID postBakingProcessor;
    //private AID dummyOrderProcesser = new AID("dummyOrderProcesser", AID.ISLOCALNAME);
    //private ArrayList<AID> ovenAgents = new ArrayList<AID>();
    private AID ovenAgent;

    private String scenarioPath;
    private AgentContainer container = null;

    private Bakery bakery;
    private String bakeryId;

    private String bakingManagerAgentName; // Name used to register in the yellowpages

    private WorkQueue needsBaking = new WorkQueue();
    private WorkQueue needsPreparation = new WorkQueue();
    private WorkQueue needsCooling = new WorkQueue();

    private HashMap<String, OrderMas> orders = new HashMap<String, OrderMas>();

    private static final String NEEDS_BAKING = "needsBaking";
    private static final String NEEDS_PREPARATION = "needsPreparation";
    private static final String NEEDS_COOLING = "needsCooling";

    private Vector<Equipment> equipment;
    private Vector<ProductMas> products;

    private int coolingRequestCounter = 0; // TODO: What?
    private AtomicInteger messageProcessing = new AtomicInteger(0);

    private AtomicBoolean isInProductionTime = new AtomicBoolean (false);

    protected void setup() {
        super.setup();

        Object[] args = getArguments();
		if (args != null && args.length > 0) {
            this.scenarioPath = (String) args[0];
            this.bakeryId = (String) args[1];
		}

        //Get the container of this agent
        container = (AgentContainer)getContainerController();
        //System.out.println("-------> Container Baking" + container);
        bakingManagerAgentName = "BakingInterface_" + bakeryId;

        // Register the Baking-manager in the yellow pages
        this.register(bakingManagerAgentName, "JADE-bakery");
        System.out.println("Hello! " + getAID().getLocalName() + " is ready.");

        //Read the scenario file and get the bakery with this.bakeryId
        getBakery(scenarioPath);

        // Get equipment for this bakery
        equipment = bakery.getEquipment();

        // Create an agent for each equipment
        createEquipmentAgents();
        createBakingPreparationAgent();
        createPostBakingProcessorAgent();

        getDummyOrderProcesserAID();
        getProoferAID();
        // getCoolingRackAID();
        getBakingPreparationAID();

        getPostBakingProcessorAID();

        addBehaviour(new timeTracker());
        addBehaviour(new ReceiveOrders());
        addBehaviour(new ReceiveDoughNotification());
        addBehaviour(new ReceiveBakingNotification());
        addBehaviour(new ReceivePreparationNotification());

        addBehaviour(new checkingBakingWorkqueue());
        addBehaviour(new checkingPreparationWorkqueue());
    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            // Check if we are allowed to do an action
            if (!baseAgent.getAllowAction()) {
                return;
            }
            // Only advance if we aren't currently processing any messages and if we are in production time
            if (messageProcessing.get() <= 0)
            {
                // Production time is from midnight to lunch (from 00.00 hrs to 12 hrs)
                if ((baseAgent.getCurrentTime().greaterThan(new Time(baseAgent.getCurrentDay(), 0, 0)) ||

                        baseAgent.getCurrentTime().equals(new Time(baseAgent.getCurrentDay(), 0, 0))) &&

                        baseAgent.getCurrentTime().lessThan(new Time(baseAgent.getCurrentDay(), 12, 0)))
                {

                    isInProductionTime.set(true);
                    //System.out.println("Setting to true");

                }
                else{

                    isInProductionTime.set(false);
                    // System.out.println("Out of production hours");
                    //System.out.println("Setting to false");
                }

                baseAgent.finished();
            }
        }
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
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

    private void createEquipmentAgents() {

         // Create one ovenAgent (which will manage all the ovens)

        String ovenAgentName = "OvenAgent_" +  bakeryId;

        ovenAgent = new AID(ovenAgentName, AID.ISLOCALNAME);

        try {
            Object[] args = new Object[4];
            args[0] = ovenAgentName;
            args[1] = bakingManagerAgentName;
            args[2] = scenarioPath;
            args[3] = bakeryId;

            AgentController ovenAgent = container.createNewAgent(ovenAgentName, "org.mas_maas.agents.OvenAgent", args);
            ovenAgent.start();

        } catch (Exception any) {
            any.printStackTrace();
        }
    }

    private void createBakingPreparationAgent(){
        String bakingPreparationAgentName = "BakingPreparationAgent_" +  bakeryId;
        try {
            Object[] args = new Object[2];
            args[0] = bakingPreparationAgentName;
            args[1] = bakingManagerAgentName;

            AgentController bakingPreparationAgent = container.createNewAgent(bakingPreparationAgentName, "org.mas_maas.agents.BakingPreparationAgent", args);
            bakingPreparationAgent.start();

        } catch (Exception any) {
            any.printStackTrace();
        }
    }

    private void createPostBakingProcessorAgent(){

        String postBakingProcessorAgentName = bakeryId + "-postBakingProcessor";
        try {
            Object[] args = new Object[3];
            args[0] = postBakingProcessorAgentName;
            args[1] = bakingManagerAgentName;
            args[2] = bakeryId;

            AgentController postBakingProcessorAgent = container.createNewAgent(postBakingProcessorAgentName, "org.mas_maas.agents.PostBakingProcessor", args);
            postBakingProcessorAgent.start();

        } catch (Exception any) {
            any.printStackTrace();
        }
    }

    public void getDummyOrderProcesserAID() {
        String dummyOrderProcesserName = "DummyOrderProcesser";
        dummyOrderProcesser = new AID(dummyOrderProcesserName, AID.ISLOCALNAME);
    }

    public void getBakingPreparationAID() {
        String bakingPreparationAgentName = "BakingPreparationAgent_" +  bakeryId;
        bakingPreparationAgent = new AID(bakingPreparationAgentName, AID.ISLOCALNAME);
    }

    public void getPostBakingProcessorAID() {
        String postBakingProcessorAgentName =  bakeryId + "-postBakingProcessor";
        postBakingProcessor = new AID(postBakingProcessorAgentName, AID.ISLOCALNAME);
    }

    public void getProoferAID() {
        String prooferAgentName = "Proofer_" + bakeryId;
        prooferAgent = new AID(prooferAgentName, AID.ISLOCALNAME);
    }

    public void getCoolingRackAID() {
        String coolingRackAgentName = "CoolingRack_" + bakeryId;
        coolingRackAgent = new AID(coolingRackAgentName, AID.ISLOCALNAME);
    }

    private class ReceiveOrders extends CyclicBehaviour {
        public void action() {

            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchSender(dummyOrderProcesser));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                // System.out.println(getAID().getLocalName() + " received order " + content +
                //     "\n \t from " + msg.getSender().getName());
                OrderMas order = JSONConverter.parseOrder(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Order was received");
                reply.setConversationId("reply-Order");
                baseAgent.sendMessage(reply);

                orders.put(order.getGuid(), order);
                // queueOrder(order);

                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    // Behaviour that checks the needsKneading workqueue and activates CFP for requesting kneading
    private class checkingBakingWorkqueue extends CyclicBehaviour{
        public void action(){
            messageProcessing.incrementAndGet();

            if (needsBaking.hasProducts() && isInProductionTime.get()){

                // Creates a bakingRequestMessage for the first product in the workqueue
                BakingRequest bakingRequestMessage = createBakingRequestMessage();

                String productType = bakingRequestMessage.getProductType();

                //Batch of ProductStatuses used for creating the bakingRequest
                Vector <ProductStatus> batch = needsBaking.findProductStatus(productType);

                //Remove the product from the needsBaking workQueue
                needsBaking.removeProductStatus(productType);

                // Convert the bakingRequest object to a String.
                Gson gson = new Gson();
                String bakingRequestString = gson.toJson(bakingRequestMessage);

                // System.out.println("----> BakingRequest: " + bakingRequestString);

                // Add behavior to send a CFP for this bakingRequest
                addBehaviour(new RequestBaking(bakingRequestString));
            }
            messageProcessing.decrementAndGet();

        }
    }

    // Behaviour that checks the needsKneading workqueue and activates CFP for requesting kneading
    private class checkingPreparationWorkqueue extends CyclicBehaviour{
        public void action(){
            messageProcessing.incrementAndGet();

            if (needsPreparation.hasProducts() && isInProductionTime.get()){

                // Creates a preparationRequestMessage for the first product in the workqueue
                PreparationRequest preparationRequestMessage = createPreparationRequestMessage();

                //System.out.println("preparationRequestMessage" + preparationRequestMessage);

                String productType = preparationRequestMessage.getProductType();

                //Batch of ProductStatuses used for creating the preparationRequest
                Vector <ProductStatus> batch = needsPreparation.findProductStatus(productType);

                //Remove the product from the needsPreparation workQueue
                needsPreparation.removeProductStatus(productType);

                // Convert the preparationRequest object to a String.
                Gson gson = new Gson();
                String preparationRequestString = gson.toJson(preparationRequestMessage);

                // System.out.println("bakingPreparationRequest: " + preparationRequestString);

                // Add behavior to send a CFP for this preparationRequest
                addBehaviour(new RequestPreparation(preparationRequestString, batch));
            }
            messageProcessing.decrementAndGet();

        }
    }

    public void queueBaking(String productType, Vector<String> guids, Vector<Integer> productQuantities ) {
        // Add productStatus to the needsBaking WorkQueue

        for (String guid : guids) {

            int amount = -1;
            String status = NEEDS_BAKING;
            ProductMas product = bakery.findProduct(productType);
            OrderMas order = orders.get(guid);

            for(BakedGood bakedGood : order.getBakedGoods()) {
                if (bakedGood.getName().equals(productType)) {
                    amount = bakedGood.getAmount();
                }

            }
            ProductStatus productStatus = new ProductStatus(guid, status, amount, product);
            needsBaking.addProduct(productStatus);
        }
    }

    public void queuePreparation(String productType, Vector<String> guids, Vector<Integer> productQuantities) {
        // Add productStatus to the needsPreparation WorkQueue

        for (int i = 0; i < guids.size(); i++) {

            //int amount = -1;
            String status = NEEDS_PREPARATION;
            ProductMas product = bakery.findProduct(productType);
            int amount = productQuantities.get(i);
            String guid = guids.get(i);
            //OrderMas order = orders.get(guid);

            ProductStatus productStatus = new ProductStatus(guid, status, amount, product);
            needsPreparation.addProduct(productStatus);
        }
    }

    public void queueCooling(String productType, Vector<String> guids, Vector <Integer> productQuantities ) {
        // Add productStatus to the needsCooling WorkQueue


        // System.out.println("++ Guids" + guids + " ProductType " + productType + "Quantities" + productQuantities);
        // System.out.println("+++++ productQuantities" + productQuantities);



        for (int i = 0; i < guids.size(); i++) {

            //int amount = -1;
            String status = NEEDS_COOLING;
            ProductMas product = bakery.findProduct(productType);
            //String guid = guids.get(i);
            int amount = productQuantities.get(i);
            //OrderMas order = orders.get(guid);

            // for(BakedGood bakedGood : order.getBakedGoods()) {
            //     if (bakedGood.getName().equals(productType)) {
            //         amount = bakedGood.getAmount();
            //     }

            //}

            // System.out.println("-- ProductType " + productType + "amount" + amount + "product" + product);

            ProductStatus productStatus = new ProductStatus(productType, status, amount, product);
            needsCooling.addProduct(productStatus);
        }
    }

    public BakingRequest createBakingRequestMessage() {
        // Checks the needsBaking WorkQueue and creates a bakingRequestMessage
        Vector<ProductStatus> products = needsBaking.getProductBatch();

        BakingRequest bakingRequest = null;

        if (products != null) {

            Vector<String> guids = new Vector<String>();
            Vector<Integer> productQuantities = new Vector<Integer>();
            Vector<Integer> slotsNeeded = new Vector<Integer>();
            int productPerSlot = 0;

            String productType = products.get(0).getProduct().getGuid();

            for (ProductStatus productStatus : products) {
                guids.add(productStatus.getGuid());
                productQuantities.add(productStatus.getAmount());
                productPerSlot = findProductPerSlot(productType);
                slotsNeeded.add((int) Math.ceil( ((float)productStatus.getAmount()/(float)productPerSlot)));
            }


            int bakingTemp = products.get(0).getProduct().getRecipe().getBakingTemp();
            float bakingTime = products.get(0).getProduct().getRecipe().getActionTime(Step.BAKING_STEP);




            bakingRequest = new BakingRequest(guids, productType, bakingTemp, slotsNeeded, bakingTime, productQuantities, productPerSlot);

        }

        return bakingRequest;

    }

    public PreparationRequest createPreparationRequestMessage() {
        // Checks the needsPreparaion WorkQueue and creates a preparationRequestMessage
        Vector<ProductStatus> products = needsPreparation.getProductBatch();

        PreparationRequest preparationRequest = null;

        if (products != null) {

            Vector<String> guids = new Vector<String>();
            Vector<Integer> productQuantities = new Vector<Integer>();
            Vector<Step> steps = new Vector<Step>();



            for (ProductStatus productStatus : products) {
                guids.add(productStatus.getGuid());
                productQuantities.add(productStatus.getAmount());
            }

            String productType = products.get(0).getProduct().getGuid();
            steps = products.get(0).getProduct().getRecipe().getBakingPreparationSteps();

            preparationRequest = new PreparationRequest(guids, productType, productQuantities, steps);
        }

        return preparationRequest;

    }

    public Vector <CoolingRequest> createCoolingRequests(String productType, int quantity) {

        Vector<CoolingRequest> coolingRequests = new Vector<CoolingRequest>();

        ProductMas product = bakery.findProduct(productType);
        float coolingDuration = product.getRecipe().getActionTime(Step.COOLING_STEP);

        CoolingRequest coolingRequest = new CoolingRequest();
        coolingRequest.addCoolingRequest(productType, coolingDuration, quantity);

        coolingRequests.add(coolingRequest);

        return coolingRequests;
    }

    public int findProductPerSlot(String productType){
        // Get the Number of products the slot can fit based on product type
        for (ProductMas product : products) {
            if (product.getGuid().equals(productType)){
                Batch batch = product.getBatch();
                return batch.getBreadsPerOven();
            }
        }
        return (int) 0;
    }


    /* This is the behavior used for receiving doughNotifications */
    private class ReceiveDoughNotification extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("dough-Notification"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("================================================================================");
                // System.out.println(getAID().getLocalName()+" Received dough Notification from \n \t" + msg.getSender()
                //     + " for: " + msg.getContent());
                // System.out.println("================================================================================");
                String doughNotificationString = msg.getContent();
                // System.out.println("Dough notification contains -> " +doughNotificationString);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Dough Notification was received");
                reply.setConversationId("dough-Notification-reply");
                baseAgent.sendMessage(reply);
                //
                //
                DoughNotification doughNotification = JSONConverter.parseDoughNotification(doughNotificationString);
                String productType = doughNotification.getProductType();
                Vector<String> guids = doughNotification.getGuids();
                Vector<Integer> productQuantities = doughNotification.getProductQuantities();
                //
                // //Add the new request to the needsBaking workqueue
                queueBaking(productType, guids, productQuantities);
                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    /* This is the behaviour used for receiving baking notification */
    private class ReceiveBakingNotification extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("baking-notification"));
            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("======================================");
               //  System.out.println("-------> " + getAID().getLocalName()+" Received Baking Notification from "
               // + msg.getSender() + " for: " + msg.getContent());
                // System.out.println("======================================");
                String bakingNotificationString = msg.getContent();

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Baking Notification was received");
                reply.setConversationId("baking-notification-reply");
                baseAgent.sendMessage(reply);

                // Convert bakingNotificationString to bakingNotification object
                BakingNotification bakingNotification = JSONConverter.parseBakingNotification(bakingNotificationString);
                String productType = bakingNotification.getProductType();
                Vector<String> guids = bakingNotification.getGuids();
                Vector<Integer> productQuantities = bakingNotification.getProductQuantities();

                // Add guids with this productType to the queuePreparation
                queuePreparation(productType, guids, productQuantities);
                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    /* This is the behavior used for receiving preparation notifications */
    private class ReceivePreparationNotification extends CyclicBehaviour {
        public void action() {

            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("preparationBaking-notification"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("======================================");
                // System.out.println(getAID().getLocalName()+" Received Baking Preparation Notification from "
//                + msg.getSender() + " for: " + msg.getContent());
                // System.out.println("======================================");
                String preparationNotificationString = msg.getContent();

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Baking Preparation Notification was received");
                reply.setConversationId("preparationBaking-notification-reply");
                baseAgent.sendMessage(reply);

                // Convert preparationNotificationString to preparationNotification object
                PreparationNotification preparationNotification = JSONConverter.parsePreparationNotification(preparationNotificationString);

                String productType = preparationNotification.getProductType();
                Vector<String> guids = preparationNotification.getGuids();
                Vector<Integer> productQuantities = preparationNotification.getProductQuantities();


                // Add guids with this productType to the queueCooling
                //queueCooling(productType, guids, productQuantities);
                // TODO:
                //Create coollingRequestMessages with the information in the queueCooling
                Gson gson = new Gson();

                for (int quantity : productQuantities){
                    Vector<CoolingRequest> coolingRequests = createCoolingRequests(productType, quantity);


                    String coolingRequestString = gson.toJson(coolingRequests);

                    // System.out.println("CoolingRequestString: " + coolingRequestString);

                    // Adds one behaviour per coolingRequest
                    addBehaviour(new RequestCooling(coolingRequestString, coolingRequestCounter));
                    coolingRequestCounter ++;





                }

                // for (CoolingRequest coolingRequest : coolingRequests) {
                //     String coolingRequestString = gson.toJson(coolingRequest);
                //     // Adds one behaviour per coolingRequest
                //     addBehaviour(new RequestCooling(coolingRequestString, coolingRequestCounter));
                //     coolingRequestCounter ++;
                // }
                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    private class RequestBaking extends Behaviour {
        private MessageTemplate mt;
            private int option = 0;
            private String bakingRequest;
            private Vector <ProductStatus> batch;
            private ArrayList<AID> ovensAvailable;
            private AID oven; // The oven that will perform kneading
            private int repliesCnt = 0;

        public RequestBaking(String bakingRequest){
            this.bakingRequest = bakingRequest;
        }

        public void action() {
            messageProcessing.incrementAndGet();

            switch (option) {
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setContent(bakingRequest);
                    msg.setConversationId("baking-request");

                    msg.addReceiver(ovenAgent);

                    baseAgent.sendMessage(msg);

                    // System.out.println(getAID().getLocalName() + " Sent bakingRequest \n \t" +
                    //         bakingRequest);
                    messageProcessing.decrementAndGet();
                    option = 1;
                    break;

                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("baking-request-reply"));

                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {
                        // System.out.println(getAID().getLocalName() + " Received baking notification confirmation from \n \t" + reply.getSender());
                        option = 2;
                    }
                    else {
                        messageProcessing.decrementAndGet();
                        block();
                    }
                    messageProcessing.decrementAndGet();
                    break;

                default:
                    messageProcessing.decrementAndGet();
                    break;
            }
        }

        public boolean done() {
            return option == 2;
        }


    }

    //This is the behaviour used for sending a PreparationRequest
    private class RequestPreparation extends Behaviour{
        private String preparationRequest;
        private Vector <ProductStatus> batch;
        private MessageTemplate mt;
        private AID bakingPreparation;
        private int option = 0;

        public RequestPreparation(String preparationRequest, Vector <ProductStatus> batch){
            this.preparationRequest = preparationRequest;
            // Batch of products used for creating the kneadingRequest
            this.batch = batch;
        }
        public void action(){
            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();

            switch(option){
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    cfp.addReceiver(bakingPreparationAgent);

                    cfp.setContent(preparationRequest);
                    cfp.setConversationId("baking-preparation-request");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis());

                    // System.out.println("======================================");
                    // System.out.println("CFP for: " + preparationRequest);
                    // System.out.println("======================================");

                    baseAgent.sendMessage(cfp);

                    // Template to get proposals/refusals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("baking-preparation-request"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    messageProcessing.decrementAndGet();
                    option = 1;
                    break;

                case 1:
                    // Receive proposals/refusals
                    ACLMessage reply = baseAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // System.out.println(getAID().getLocalName() + " received a proposal from " +
                            //     reply.getSender().getName() + " for: " + proofingRequest);


                        // }else if (reply.getPerformative() == ACLMessage.FAILURE){
                        //     option = 0;
                        //
                        // }

                            bakingPreparation = reply.getSender();

                        }
                        option = 2;
                        messageProcessing.decrementAndGet();
                    }

                    else {
                        messageProcessing.decrementAndGet();
                        block();
                    }
                    break;

            case 2:
                    ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

                    msg.addReceiver(bakingPreparation);

                    // System.out.println(getAID().getLocalName() + " Accepting proposal from "
                    //     + preparationTable.getName() + " for: " + preparationRequest);

                    msg.setContent(preparationRequest);
                    msg.setConversationId("baking-preparation-request");
                    msg.setReplyWith(preparationRequest + System.currentTimeMillis());
                    baseAgent.sendMessage(msg);

                    // Prepare the template to get the msg reply
                    mt = MessageTemplate.and(
                         MessageTemplate.MatchConversationId("baking-preparation-request"),
                         MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                    option = 3;
                    messageProcessing.decrementAndGet();
                    break;

            case 3:
                // Receive the confirmation from the preparationTable
                reply = baseAgent.receive(mt);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        // System.out.println(getAID().getLocalName()+ " baking preparation confirmation received from -> \n \t"
                        //     +reply.getSender().getLocalName() + " for: " + reply.getContent());
                        }
                        else{
                            // System.out.println(getAID().getLocalName() + " baking rejection received from -> "
                            //     +reply.getSender().getLocalName() + " for: " + preparationRequest);

                            //Add the batch to the needsPreparation queue
                            for (ProductStatus productStatus : batch){
                                needsPreparation.addProduct(productStatus);
                            }
                        }
                        option = 4;
                        messageProcessing.decrementAndGet();
                }
                else {
                    messageProcessing.decrementAndGet();
                    block();
                }
                break;

            default:
                messageProcessing.decrementAndGet();
                break;
            }
        }
        public boolean done(){
            if (option == 2 && bakingPreparation == null) {
                // System.out.println("++++++Attempt failed for " + preparationRequest + "Adding request to the needsPreparation queue");
                //Add the batch to the needsKneading queue
                for (ProductStatus productStatus : batch){
                    needsPreparation.addProduct(productStatus);
                }
            }
            return ((option == 2 && bakingPreparation == null) || option == 4);
        }
    }


    //This is the behaviour used for sending a CoolingRequest
    private class RequestCooling extends Behaviour{
        private String coolingRequest;
        private int coolingRequestcounter;
        private MessageTemplate mt;
        private int option = 0;

        public RequestCooling(String coolingRequest, int coolingRequestCounter){
            this.coolingRequest = coolingRequest;
            this.coolingRequestcounter = coolingRequestCounter;
        }
        public void action(){

            messageProcessing.incrementAndGet();
            switch(option){
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    // hacky way to remove the charaters before the json array section of the string and the '}' after
                    coolingRequest = coolingRequest.replaceFirst(".*?:", "");
                    coolingRequest = coolingRequest.substring(0, coolingRequest.length() - 1);

                    // System.out.println("Cooling request: " + coolingRequest);


                    msg.setContent(coolingRequest);
                    msg.setConversationId("baked-products-" + coolingRequestCounter);
                    msg.addReceiver(postBakingProcessor);

                    // Send bakingRequest msg to all ovenAgents
                    // for (int i=0; i<coolingRackAgent.length; i++){
                    //     msg.addReceiver(coolingRackAgent[i]);
                    // }
                    // msg.setReplyWith("msg"+System.currentTimeMillis());
                    baseAgent.sendMessage(msg);  // calling sendMessage instead of send

                    option = 1;
                    // System.out.println(getLocalName()+" Sent coolingRequest" + coolingRequest);
                    messageProcessing.decrementAndGet();
                    break;

            default:
                messageProcessing.decrementAndGet();
                break;
            }
        }
        public boolean done(){
            return (option == 1);
        }

    }

}
