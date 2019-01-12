package org.mas_maas.agents;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.maas.JSONConverter;
import org.maas.messages.KneadingNotification;
import org.maas.messages.KneadingRequest;
import org.maas.messages.PreparationNotification;
import org.maas.messages.PreparationRequest;
import org.maas.messages.ProofingRequest;
import org.maas.Objects.BakedGood;
import org.maas.Objects.Bakery;
import org.maas.Objects.Client;
import org.maas.Objects.DoughPrepTable;
import org.maas.Objects.Equipment;
import org.maas.Objects.KneadingMachine;
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

public class DoughManager extends BaseAgent {
    private AID dummyOrderProcesserAgent;
    private AID prooferAgent;
    private AID [] kneadingMachineAgents;
    private AID [] preparationTableAgents;
    private AID dummyOrderProcesser = new AID("dummyOrderProcesser", AID.ISLOCALNAME);
    private String scenarioPath;
    private AgentContainer container = null;

    private Bakery bakery;
    private String bakeryId;

    private String doughManagerAgentName; // Name used to register in the yellowpages
    private WorkQueue needsKneading = new WorkQueue();
    private WorkQueue needsPreparation = new WorkQueue();
    private WorkQueue needsProofing = new WorkQueue();
    private HashMap<String, OrderMas> orders = new HashMap<String, OrderMas>();
    private static final String NEEDS_KNEADING = "needsKneading";
    private static final String NEEDS_PREPARATION = "needsPreparation";
    private static final String NEEDS_PROOFING = "needsProofing";
	private Vector<Equipment> equipment;
    private Vector<String> kneadingMachineNames = new Vector<String>();
    private Vector<String> doughPrepTableNames = new Vector<String>();
    private AtomicInteger messageProcessing = new AtomicInteger(0);

    protected void setup() {
        super.setup();

        Object[] args = getArguments();
		if (args != null && args.length > 0) {
            this.scenarioPath = (String) args[0];
            this.bakeryId = (String) args[1];
		}

        //Get the container of this agent
        container = (AgentContainer)getContainerController();
        doughManagerAgentName = "DoughManagerAgent_" + bakeryId;

        // Register the Dough-manager in the yellow pages
        this.register(doughManagerAgentName, "JADE-bakery");
        System.out.println("Hello! " + getAID().getLocalName() + " is ready.");

        //Read the scenario file and get the bakery with this.bakeryId
        getBakery(scenarioPath);
        //System.out.println("Bakery " + bakeryId + " is " + bakery.getGuid());

        // Get equipment for this bakery
		equipment = bakery.getEquipment();
		// Create an agent for each equipment
		createEquipmentAgents();

		getDummyOrderProcesserAID();
		getProoferAID();
        getKneadingMachineAIDs();
        getPreparationTableAIDS();

        // Time tracker behavior
        addBehaviour(new timeTracker());

        // Activate behavior that receives orders
        addBehaviour(new ReceiveOrders());

        addBehaviour(new ReceiveKneadingNotification());
        addBehaviour(new ReceivePreparationNotification());


    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }

            // only advance if we aren't currently processing any messages
            if (messageProcessing.get() <= 0)
            {
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

    }

    private void createEquipmentAgents() {

        for (int i = 0; i < equipment.size(); i++){

            // Create KneadingMachineAgents agents for this bakery
            if (equipment.get(i) instanceof KneadingMachine){

                // Object of type KneadingMachine
                KneadingMachine kneadingMachine = (KneadingMachine) equipment.get(i);
                // Name of the kneadingMachineAgent
                String kneadingMachineAgentName = "KneadingMachineAgent_" +  bakeryId + "_" + kneadingMachine.getGuid();

                kneadingMachineNames.add(kneadingMachineAgentName);

                try {
                    Object[] args = new Object[3];
                    args[0] = kneadingMachine;
                    args[1] = kneadingMachineAgentName;
                    args[2] = doughManagerAgentName;

                    AgentController kneadingMachineAgent = container.createNewAgent(kneadingMachineAgentName, "org.mas_maas.agents.KneadingMachineAgent", args);
                    kneadingMachineAgent.start();

                    // System.out.println(getLocalName()+" created and started:"+ kneadingMachineAgent + " on container "+((ContainerController) container).getContainerName());

                } catch (Exception any) {
                    any.printStackTrace();
                }
            }


            //Create DougPrepTable agents for this bakery
            if (equipment.get(i) instanceof DoughPrepTable){

                //Object of type DoughPrepTable
                DoughPrepTable doughPrepTable = (DoughPrepTable) equipment.get(i);
                //Name of preparationTableAgent

                String doughPrepTableAgentName = "DoughPrepTableAgent_" +  bakeryId + "_" + doughPrepTable.getGuid();

                doughPrepTableNames.add(doughPrepTableAgentName);

                try {
                    Object[] args = new Object[3];
                     args[0] = doughPrepTable;
                     args[1] = doughPrepTableAgentName;
                     args[2] = doughManagerAgentName;

                    AgentController preparationTableAgent = container.createNewAgent(doughPrepTableAgentName, "org.mas_maas.agents.PreparationTableAgent", args);
                    preparationTableAgent.start();

                    // System.out.println(getLocalName()+" created and started:"+ preparationTableAgent + " on container "+((ContainerController) container).getContainerName());
                } catch (Exception any) {
                    any.printStackTrace();
                }
                }

		}

	}

    public void getDummyOrderProcesserAID() {
        String dummyOrderProcesserAgentName = "DummyOrderProcesser";
        dummyOrderProcesserAgent = new AID(dummyOrderProcesserAgentName, AID.ISLOCALNAME);
    }

    public void getProoferAID() {
        String prooferAgentName = "Proofer_" + bakeryId;
        prooferAgent = new AID(prooferAgentName, AID.ISLOCALNAME);
    }

    public void getKneadingMachineAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        kneadingMachineAgents = new AID [kneadingMachineNames.size()];

        int j = 0;

        for(String kneadingMachineName : kneadingMachineNames) {
        	sd.setType(kneadingMachineName);
            template.addServices(sd);

            try {
                DFAgentDescription [] result = DFService.search(this, template);
                //System.out.println(getAID().getLocalName() + " Found the following Kneading-machine agents:");

                for (int i = 0; i < result.length; ++i) {
                    kneadingMachineAgents[j] = result[i].getName();
                    //System.out.println(kneadingMachineAgents[j].getName());
                }

            }
            catch (FIPAException fe) {
                System.out.println("----> NOT FOUND " + kneadingMachineName);
                fe.printStackTrace();
            }
            j ++;
        }

    }

    public void getPreparationTableAIDS() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        preparationTableAgents = new AID [doughPrepTableNames.size()];

        int j = 0;

        for(String doughPrepTableName : doughPrepTableNames) {
        	sd.setType(doughPrepTableName);
            template.addServices(sd);

            try {
                DFAgentDescription [] result = DFService.search(this, template);
                //System.out.println(getAID().getLocalName() + " Found the following doughPrepTable agents:");

                for (int i = 0; i < result.length; ++i) {
                    preparationTableAgents[j] = result[i].getName();
                    //System.out.println(preparationTableAgents[j].getName());
                }

            }
            catch (FIPAException fe) {
                System.out.println("----> NOT FOUND " + doughPrepTableName);
                fe.printStackTrace();
            }
            j ++;
        }
    }



    public void queueOrder(OrderMas order) {
        // Add productStatus to the needsKneading WorkQueue

        for(BakedGood bakedGood : order.getBakedGoods()) {

            int amount = bakedGood.getAmount();
            if (amount > 0){
                String guid = order.getGuid();
                String status = NEEDS_KNEADING;
                ProductMas product = bakery.findProduct(bakedGood.getName());
                ProductStatus productStatus = new ProductStatus(guid, status, amount, product);

                needsKneading.addProduct(productStatus);

                KneadingRequest kneadingRequestMessage = createKneadingRequestMessage();

			    // Convert the kneadingRequest object to a String.
                Gson gson = new Gson();
                String kneadingRequestString = gson.toJson(kneadingRequestMessage);

                // Add behavior to send the kneadingRequest to the Kneading Agents
                addBehaviour(new RequestKneading(kneadingRequestString));
            }
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
            float kneadingTime = products.get(0).getProduct().getRecipe().getActionTime(Step.KNEADING_STEP);

            kneadingRequest = new KneadingRequest(guids, productType, kneadingTime);
        }

        return kneadingRequest;
    }

    public void queuePreparation(String productType, Vector<String> guids ) {
        // Add productStatus to the needsPreparation WorkQueue

        for (String guid : guids) {

            int amount = -1;
            String status = NEEDS_PREPARATION;
            ProductMas product = bakery.findProduct(productType);
            OrderMas order = orders.get(guid);

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
            ProductMas product = bakery.findProduct(productType);
            OrderMas order = orders.get(guid);

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
            Vector<Integer> productQuantities = new Vector<Integer>();

            for (ProductStatus productStatus : products) {
                guids.add(productStatus.getGuid());
                productQuantities.add(productStatus.getAmount());
            }

            String productType = products.get(0).getProduct().getGuid();

            float proofingTime = products.get(0).getProduct().getRecipe().getActionTime(Step.PROOFING_STEP);

            proofingRequest = new ProofingRequest(productType, guids, proofingTime, productQuantities);
        }

        return proofingRequest;

    }




    /* This is the behavior used for receiving orders */
    private class ReceiveOrders extends CyclicBehaviour {
        public void action() {

            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchSender(dummyOrderProcesser));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String content = msg.getContent();
                System.out.println(getAID().getLocalName() + "received order " + content + " from " + msg.getSender().getName());
                OrderMas order = JSONConverter.parseOrder(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Order was received");
                reply.setConversationId("reply-Order");
                baseAgent.sendMessage(reply);

                orders.put(order.getGuid(), order);
                queueOrder(order);
                // TODO Trigger the doughPreparation (send Kneading request, etc)
                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    /* This is the behavior used for receiving kneading notification messages */
    private class ReceiveKneadingNotification extends CyclicBehaviour {
        public void action() {

            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("kneading-notification"));
            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {

                System.out.println("-------> " + getAID().getLocalName()+" Received Kneading Notification from " + msg.getSender());
                String kneadingNotificationString = msg.getContent();
                // System.out.println("-----> Kneading notification " + kneadingNotificationString);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Kneading Notification was received");
                reply.setConversationId("kneading-notification-reply");
                baseAgent.sendMessage(reply);

                // Convert kneadingNotificationString to kneadingNotification object
                KneadingNotification kneadingNotification = JSONConverter.parseKneadingNotification(kneadingNotificationString);
                String productType = kneadingNotification.getProductType();
                Vector<String> guids = kneadingNotification.getGuids();

                // System.out.println("-----> product type " + productType);
                // System.out.println("-----> guid " + guids);

                // Add guids with this productType to the queuePreparation
                queuePreparation(productType, guids);

                // Create preparationRequestMessage with the information in the queuePreparation
                PreparationRequest preparationRequestMessage = createPreparationRequestMessage();

                // Convert preparationRequestMessage to String
                Gson gson = new Gson();

                String preparationRequestString = gson.toJson(preparationRequestMessage);

                // Send preparationRequestMessage
                addBehaviour(new RequestPreparation(preparationRequestString));
                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    /* This is the behaviour used for receiving preparation notification */
    private class ReceivePreparationNotification extends CyclicBehaviour {
        public void action() {

            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("preparation-notification"));
            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {

                System.out.println("----> " + getAID().getLocalName()+" Received Preparation Notification from " + msg.getSender());
                String preparationNotificationString = msg.getContent();

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Preparation Notification was received");
                reply.setConversationId("preparation-notification-reply");
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
                addBehaviour(new RequestProofing(proofingRequestString, prooferAgent));
                messageProcessing.decrementAndGet();
            }
            else {
                block();
                messageProcessing.decrementAndGet();
            }
        }


    }

    //This is the behaviour used for sensing a KneadingRequest
    private class RequestKneading extends Behaviour{
        private String kneadingRequest;
        private MessageTemplate mt;
        private ArrayList<AID> kneadingMachinesAvailable = new ArrayList<AID>(); // The kneadingMachineAgent that will perform kneading
        private int repliesCnt = 0;
        private int option = 0;

        public RequestKneading(String kneadingRequest){
            this.kneadingRequest = kneadingRequest;
        }

        public void action(){
            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();
            switch(option){
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    // Send kneadingRequest msg to all kneadingMachineAgents
                    for (int i=0; i<kneadingMachineAgents.length; i++){
                        cfp.addReceiver(kneadingMachineAgents[i]);
                    }

                    cfp.setContent(kneadingRequest);
                    cfp.setConversationId("kneading-request");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis());

                    System.out.println("CFP for: " + kneadingRequest);

                    baseAgent.sendMessage(cfp);  // calling sendMessage instead of send


                    // Template to get proposals/refusals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("kneading-request"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    messageProcessing.decrementAndGet();
                    option = 1;
                    break;

                case 1:

                    // mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                    //     MessageTemplate.MatchConversationId("kneading-request-reply"));

                    // Receive proposals/refusals
                    ACLMessage reply = baseAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        // The kneadingMachine that replies first gets the job
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            kneadingMachinesAvailable.add(reply.getSender());
                            // if (repliesCnt == 1){
                            //     kneadingMachine = reply.getSender();
                            System.out.println(getAID().getLocalName() + " received a proposal from " + reply.getSender().getName() + " for: " + kneadingRequest);
                            // }

                        }
                        // We received all replies
                        if (repliesCnt >= kneadingMachineAgents.length) {
                        	option = 2;

    					}
                        messageProcessing.decrementAndGet();
                    }

                    else {
                        messageProcessing.decrementAndGet();
                        block();
                    }
                    break;

                case 2:
                    if (!kneadingMachinesAvailable.isEmpty()){
                        // Accept proposal from the kneading machine that replied first
                        ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        msg.addReceiver(kneadingMachinesAvailable.get(0));
                        kneadingMachinesAvailable.remove(0);
                        msg.setContent(kneadingRequest);
                        msg.setConversationId("kneading-request");
                        msg.setReplyWith("msg"+System.currentTimeMillis());
                        baseAgent.sendMessage(msg);
                        // Prepare the template to get the msg reply
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("kneading-request"),
                    		  MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                        option = 3;
                        messageProcessing.decrementAndGet();
                    }
                    break;

                case 3:
                    // Receive the confirmation for the kneadingMachine
                    reply = baseAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                        	System.out.println(getAID().getLocalName()+ " confirmation received from -> "
                                +reply.getSender().getLocalName() + " for: " + kneadingRequest);
                            option = 4;
                        }
                        else {
                            System.out.println(getAID().getLocalName() + " rejection received from -> "
                                +reply.getSender().getLocalName() + " for: " + kneadingRequest);
                            if (!kneadingMachinesAvailable.isEmpty()){
                                option = 2;
                            }else{
                                //option = 0;
                                // All machines are unavailable. Try in the next time step.
                                option = 4;

                            }
                            // Send a knew kneading request for the failed attempt
                        }
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
            if (option == 4){
                return true;
            }
            return false;
        }
    }

    //This is the behaviour used for sending a PreparationRequest
    private class RequestPreparation extends Behaviour{
        private String preparationRequest;
        private MessageTemplate mt;
        private ArrayList<AID> preparationTablesAvailable = new ArrayList<AID>();
        private int repliesCnt = 0;
        private int option = 0;

        public RequestPreparation(String preparationRequest){
            this.preparationRequest = preparationRequest;
        }
        public void action(){
            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();

            switch(option){
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    // Send preparation requests msg to all preparationTableAgents
                    for (int i=0; i<preparationTableAgents.length; i++){
                        cfp.addReceiver(preparationTableAgents[i]);
                    }

                    cfp.setContent(preparationRequest);
                    cfp.setConversationId("preparation-request");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis());

                    System.out.println("CFP for: " + preparationRequest);

                    baseAgent.sendMessage(cfp);

                    // Template to get proposals/refusals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("preparation-request"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    messageProcessing.decrementAndGet();
                    option = 1;
                    break;

                case 1:
                ACLMessage reply = baseAgent.receive(mt);
                if (reply != null) {
                    repliesCnt++;
                    // The doughPrepTable that replies first gets the job
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        preparationTablesAvailable.add(reply.getSender());
                        System.out.println(getAID().getLocalName() + " received a proposal from " + reply.getSender().getName() + " for: " + preparationRequest);
                    }
                    // We received all replies
                    if (repliesCnt >= preparationTableAgents.length) {
                        option = 2;
                    }
                    messageProcessing.decrementAndGet();
                }

                else {
                    messageProcessing.decrementAndGet();
                    block();
                }
                break;

            case 2:
                if (!preparationTablesAvailable.isEmpty()){
                    // Accept proposal from the preparationTable that replied first
                    ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    msg.addReceiver(preparationTablesAvailable.get(0));
                    preparationTablesAvailable.remove(0);
                    msg.setContent(preparationRequest);
                    msg.setConversationId("preparation-request");
                    msg.setReplyWith("msg"+System.currentTimeMillis());
                    baseAgent.sendMessage(msg);
                    // Prepare the template to get the msg reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("preparation-request"),
                          MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    option = 3;
                    messageProcessing.decrementAndGet();
                }
                break;

            case 3:
                // Receive the confirmation from the prepTable
                reply = baseAgent.receive(mt);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.CONFIRM) {
                        System.out.println(getAID().getLocalName()+ " confirmation received from -> "
                            +reply.getSender().getLocalName() + " for: " + preparationRequest);
                        option = 4;
                    }
                    else {
                        System.out.println(getAID().getLocalName() + " rejection received from -> "
                            +reply.getSender().getLocalName() + " for: " + preparationRequest);
                        if (!preparationTablesAvailable.isEmpty()){
                            option = 2;
                        }else{
                            //option = 0;
                            // All machines are unavailable. Try in the next time step.
                            option = 4;

                        }
                    }
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
            if (option == 4){
                return true;
            }
            return false;
        }
    }

    // This is the behavior used for sensing a ProofingRequest
    private class RequestProofing extends Behaviour{
        private String proofingRequest;
        private AID prooferAgent;
        private MessageTemplate mt;
        private boolean proofingRequested = false;
        private int option = 0;

        public RequestProofing(String proofingRequest, AID prooferAgent){
            this.proofingRequest = proofingRequest;
            this.prooferAgent = prooferAgent;
        }
        public void action(){
            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();

            switch(option){
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(proofingRequest);
                    msg.setConversationId("proofing-request");

                    // Send proofingRequest msg to all prooferAgents
                    //for (int i=0; i<prooferAgents.length; i++){
                    //    msg.addReceiver(prooferAgents[i]);
                    //}
                    msg.addReceiver(prooferAgent);
                    // msg.setReplyWith("msg"+System.currentTimeMillis());
                    baseAgent.sendMessage(msg);  // calling sendMessage instead of send

                    option = 1;
                    proofingRequested = true;
                    System.out.println("-----> " + getLocalName()+" Sent proofingRequest" + proofingRequest);
                    messageProcessing.decrementAndGet();
                    break;

                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("proofing-request-reply"));
                    ACLMessage reply = baseAgent.receive(mt);


                    if (reply != null) {
                        System.out.println("-----> " +getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                        option = 2;
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
            if (option == 2){
                proofingRequested = false;
                // For now the DoughManager terminates after processing one order
                //System.out.println(getAID().getLocalName() + " My life is over ");
                //baseAgent.finished();
                //baseAgent.doDelete();
                return true;

            }
            return false;
        }
    }

}
