package org.mas_maas.agents;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.KneadingNotification;
import org.mas_maas.messages.KneadingRequest;
import org.mas_maas.messages.PreparationNotification;
import org.mas_maas.messages.PreparationRequest;
import org.mas_maas.messages.ProofingRequest;
import org.mas_maas.objects.BakedGood;
import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.Order;
import org.mas_maas.objects.Product;
import org.mas_maas.objects.ProductStatus;
import org.mas_maas.objects.Step;
import org.mas_maas.objects.WorkQueue;

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


public class DoughManager extends BaseAgent {
    private AID [] orderProcessingAgents;
    private AID [] prooferAgents;
    private AID [] preparationTableAgents;
    private AID [] kneadingMachineAgents;
    private Bakery bakery;
    private WorkQueue needsKneading;
    private WorkQueue needsPreparation;
    private WorkQueue needsProofing;
    private HashMap<String, Order> orders = new HashMap<String, Order>();
    private static final String NEEDS_KNEADING = "needsKneading";
    private static final String NEEDS_PREPARATION = "needsPreparation";
    private static final String NEEDS_PROOFING = "needsProofing";


    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");

        // Load bakery information (includes recipes for each product)
        getbakery();

        // Queue of productStatus which require kneading
        needsKneading = new WorkQueue();
        // Queue of productStatus which require preparation (resting, item preparation, ...)
        needsPreparation = new WorkQueue();
        // Queue of productStatus which require proofing
        needsProofing = new WorkQueue();

        // Register the Dough-manager in the yellow pages
        this.register("Dough-manager", "JADE-bakery");

        this.getOrderProcessingAIDs();
        this.getProoferAIDs();
        this.getPreparationTableAIDS();
        this.getKneadingMachineAIDs();

        // Activate behavior that receives orders
        addBehaviour(new ReceiveOrders());

        // For now, the orderProcessingAgents do not exist. The manager has an order object.

        // Create order object
        
        String productName = "Bagel";
        int amount = 5;
        BakedGood bakedGood = new BakedGood(productName, amount);

        String customerId = "001";
        String guid = "order-001";
        int orderDay = 12;
        int orderHour = 4;
        int deliveryDate = 13;
        int deliveryHour = 4;
        Vector<BakedGood> bakedGoods = new Vector<BakedGood>();
        bakedGoods.add(bakedGood);
        Order order = new Order(customerId, guid, orderDay, orderHour, deliveryDate, deliveryHour, bakedGoods);
        
        System.out.println(getAID().getName() + " received the order " + order);
        
        orders.put(order.getGuid(), order);

        // Add order to the needKneading WorkQueue
        queueOrder(order);
        
        // System.out.println("Needs kneading queue " + needsKneading.getFirstProduct().getGuid());
        
        KneadingRequest kneadingRequestMessage = createKneadingRequestMessage();

        // Convert the kneadingRequest object to a String.
        Gson gson = new Gson();
        String kneadingRequestString = gson.toJson(kneadingRequestMessage);

        // Add behavior to send the kneadingRequest to the Kneading Agents
        addBehaviour(new RequestKneading( kneadingRequestString, kneadingMachineAgents));
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void queueOrder(Order order) {
        // Add productStatus to the needsKneading WorkQueue

        for(BakedGood bakedGood : order.getBakedGoods()) {

            String guid = order.getGuid();
            String status = NEEDS_KNEADING;
            int amount = bakedGood.getAmount();
            Product product = bakery.findProduct(bakedGood.getName());
            ProductStatus productStatus = new ProductStatus(guid, status, amount, product);

            needsKneading.addProduct(productStatus);

        }
    }

    public KneadingRequest createKneadingRequestMessage() {
        // Checks the needsKneading workqueue and creates a KneadingRequestMessage

        Vector<ProductStatus> products = needsKneading.getProductBatch();
        KneadingRequest kneadingRequest = null;

        if (products != null) {

            Vector<String> guids = new Vector<String>();

            for (ProductStatus productStatus : products) {
                guids.add(productStatus.getGuid());

            }
            String productType = products.get(0).getProduct().getGuid();
            float kneadingTime = products.get(0).getProduct().getRecipe().getActionTime(Step.KNEADING_TIME);

            kneadingRequest = new KneadingRequest(guids, productType, kneadingTime);
        }

        return kneadingRequest;
    }

    public void queuePreparation(String productType, Vector<String> guids ) {
        // Add productStatus to the needsPreparation WorkQueue

        for (String guid : guids) {

            int amount = -1;
            String status = NEEDS_PREPARATION;
            Product product = bakery.findProduct(guid);
            Order order = orders.get(guid);

            for(BakedGood bakedGood : order.getBakedGoods()) {
                if (bakedGood.getName().equals(productType)) {
                    amount = bakedGood.getAmount();
                }

            }
            ProductStatus productStatus = new ProductStatus(guid, status, amount, product);
            needsPreparation.addProduct(productStatus);
        }
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
            steps = products.get(0).getProduct().getRecipe().getPreparationSteps();

            preparationRequest = new PreparationRequest(guids, productType, productQuantities, steps);
        }

        return preparationRequest;

    }

    public void queueProofing(String productType, Vector<String> guids ) {
        // Add productStatus to the needsProofing WorkQueue

        for (String guid : guids) {

            int amount = -1;
            String status = NEEDS_PROOFING;
            Product product = bakery.findProduct(guid);
            Order order = orders.get(guid);

            for(BakedGood bakedGood : order.getBakedGoods()) {
                if (bakedGood.getName().equals(productType)) {
                    amount = bakedGood.getAmount();
                }

            }
            ProductStatus productStatus = new ProductStatus(guid, status, amount, product);
            needsProofing.addProduct(productStatus);
        }
    }

    public ProofingRequest createProofingRequestMessage() {
        // Checks the needsProofing WorkQueue and creates a proofingRequestMessage
        Vector<ProductStatus> products = needsProofing.getProductBatch();

        ProofingRequest proofingRequest = null;

        if (products != null) {

            Vector<String> guids = new Vector<String>();

            for (ProductStatus productStatus : products) {
                guids.add(productStatus.getGuid());
            }

            String productType = products.get(0).getProduct().getGuid();

            float proofingTime = products.get(0).getProduct().getRecipe().getActionTime(Step.PROOFING_TIME);

            proofingRequest = new ProofingRequest(productType, guids, proofingTime);
        }

        return proofingRequest;

    }

    public void getbakery(){

        String jsonDir = "src/main/resources/config/dough_stage_communication/";
        try {
            System.out.println("Working Directory = " + System.getProperty("user.dir"));
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
    }

    public void getOrderProcessingAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Order-processing");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Order-processing agents:");
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

    public void getProoferAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Proofer");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Proofer agents:");
            prooferAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                prooferAgents[i] = result[i].getName();
                System.out.println(prooferAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public void getPreparationTableAIDS() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Preparation-table");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Preparation-table agents:");
            preparationTableAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                preparationTableAgents[i] = result[i].getName();
                System.out.println(preparationTableAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public void getKneadingMachineAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Kneading-machine");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Kneading-machine agents:");
            kneadingMachineAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                kneadingMachineAgents[i] = result[i].getName();
                System.out.println(kneadingMachineAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    /* This is the behavior used for receiving orders */
    private class ReceiveOrders extends CyclicBehaviour {
        public void action() {
            // baseAgent.finished(); //call it if there are no generic behaviours
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Order was received");
                baseAgent.sendMessage(reply);
                // TODO convert String to order object
                // Add the order to the HashMap of orders. Trigger the doughPreparation (send Kneading request, etc)

            }
            else {
                block();
            }
        }
    }

    /* This is the behavior used for receiving kneading notification messages */
    private class ReceiveKneadingNotification extends CyclicBehaviour {
        public void action() {
            // baseAgent.finished(); //call it if there are no generic behaviours
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String kneadingNotificationString = msg.getContent();
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Kneading Notification was received");
                baseAgent.sendMessage(reply);

                // Convert kneadingNotificationString to kneadingNotification object
                KneadingNotification kneadingNotification = JSONConverter.parseKneadingNotification(kneadingNotificationString);

                String productType = kneadingNotification.getProductType();
                Vector<String> guids = kneadingNotification.getGuids();

                // Add guids with this productType to the queuePreparation
                queuePreparation(productType, guids);

                // Create preparationRequestMessage with the information in the queuePreparation
                PreparationRequest preparationRequestMessage = createPreparationRequestMessage();

                // Convert preparationRequestMessage to String
                Gson gson = new Gson();
                String preparationRequestString = gson.toJson(preparationRequestMessage);

                // Send preparationRequestMessage
                addBehaviour(new RequestPreparation(preparationRequestString, preparationTableAgents));

            }
            else {
                block();
            }
        }


    }

    /* This is the behaviour used for receiving preparation notification */
    private class ReceivePreparationNotification extends CyclicBehaviour {
        public void action() {
            // baseAgent.finished(); //call it if there are no generic behaviours
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String preparationNotificationString = msg.getContent();
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Preparation Notification was received");
                baseAgent.sendMessage(reply);

                // Convert preparationNotificationString to preparationNotification object

                PreparationNotification preparationNotification = JSONConverter.parsePreparationNotification(preparationNotificationString);

                String productType = preparationNotification.getProductType();
                Vector<String> guids = preparationNotification.getGuids();

                // Add guids with this productType to the queueProofing
                queueProofing(productType, guids);

                // Create proofingRequestMessage with the information in the queueProofing
                ProofingRequest proofingRequestMessage = createProofingRequestMessage();

                // Convert proofingRequestMessage to String
                Gson gson = new Gson();
                String proofingRequestString = gson.toJson(proofingRequestMessage);

                // Send preparationRequestMessage
                addBehaviour(new RequestProofing(proofingRequestString, prooferAgents));


            }
            else {
                block();
            }
        }


    }

    //This is the behaviour used for sensing a KneadingRequest
    private class RequestKneading extends Behaviour{
        private String kneadingRequest;
        private AID [] kneadingMachineAgents;
        private MessageTemplate mt;
        private ACLMessage msg;
        private int step = 0;

        public RequestKneading(String kneadingRequest, AID [] kneadingMachineAgents){
            this.kneadingRequest = kneadingRequest;
            this.kneadingMachineAgents = kneadingMachineAgents;
        }
        public void action(){
            // blocking action
            // if (!baseAgent.getAllowAction()) {
            //     return;
            // }
            switch(step){
            case 0:
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(kneadingRequest);
                msg.setConversationId("kneading-request");
                // Send kneadingRequest msg to all kneadingMachineAgents
                for (int i=0; i<kneadingMachineAgents.length; i++){
                    msg.addReceiver(kneadingMachineAgents[i]);
                }
                msg.setReplyWith("msg"+System.currentTimeMillis());
                baseAgent.sendMessage(msg);  // calling sendMessage instead of send
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("kneading-request"),
                        MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                System.out.println(getLocalName()+" Sent kneadingRequest" + kneadingRequest);
                step = 1;
                break;

            default:
                break;
            }
        }
        public boolean done(){
            if (step == 1){
                baseAgent.finished();
                return true;

            }
            return false;
        }
    }

    //This is the behaviour used for sending a PreparationRequest
    private class RequestPreparation extends Behaviour{
        private String preparationRequest;
        private AID [] preparationTableAgents;
        private MessageTemplate mt;
        private ACLMessage msg;
        private int step = 0;

        public RequestPreparation(String preparationRequest, AID [] preparationTableAgents){
            this.preparationRequest = preparationRequest;
            this.preparationTableAgents = preparationTableAgents;
        }
        public void action(){
            // blocking action
            // if (!baseAgent.getAllowAction()) {
            //     return;
            // }
            switch(step){
            case 0:
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(preparationRequest);
                msg.setConversationId("preparation-request");
                // Send kneadingRequest msg to all preparationTableAgents
                for (int i=0; i<preparationTableAgents.length; i++){
                    msg.addReceiver(preparationTableAgents[i]);
                }
                msg.setReplyWith("msg"+System.currentTimeMillis());
                baseAgent.sendMessage(msg);  // calling sendMessage instead of send
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("preparation-request"),
                        MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                System.out.println(getLocalName()+" Sent preparationRequest" + preparationRequest);
                step = 1;
                break;

            default:
                break;
            }
        }
        public boolean done(){
            if (step == 1){
                baseAgent.finished();
                return true;

            }
            return false;
        }
    }

    // This is the behavior used for sensing a ProofingRequest
    private class RequestProofing extends Behaviour{
        private String proofingRequest;
        private AID [] prooferAgents;
        private MessageTemplate mt;
        private ACLMessage msg;
        private int step = 0;

        public RequestProofing(String proofingRequest, AID [] prooferAgents){
            this.proofingRequest = proofingRequest;
            this.prooferAgents = prooferAgents;
        }
        public void action(){
            // blocking action
            // if (!baseAgent.getAllowAction()) {
            //     return;
            // }
            switch(step){
            case 0:
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(proofingRequest);
                msg.setConversationId("proofing-request");
                // Send proofingRequest msg to all prooferAgents
                for (int i=0; i<prooferAgents.length; i++){
                    msg.addReceiver(prooferAgents[i]);
                }
                msg.setReplyWith("msg"+System.currentTimeMillis());
                baseAgent.sendMessage(msg);  // calling sendMessage instead of send
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("proofing-request"),
                        MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                System.out.println(getLocalName()+" Sent proofingRequest" + proofingRequest);
                step = 1;
                break;

            default:
                break;
            }
        }
        public boolean done(){
            if (step == 1){
                baseAgent.finished();
                // For now the DoughManager terminates after processing one order
                baseAgent.doDelete();
                return true;

            }
            return false;
        }
    }

}
