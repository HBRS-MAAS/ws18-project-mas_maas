package org.mas_maas.agents;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.DoughNotification;
import org.mas_maas.messages.BakingRequest;
import org.mas_maas.messages.BakingNotification;
import org.mas_maas.messages.PreparationRequest;
import org.mas_maas.messages.PreparationNotification;
import org.mas_maas.messages.CoolingRequest;
import org.mas_maas.messages.LoadingBayMessage;
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

public class BakingManager extends BaseAgent {
	private Bakery bakery;
	private HashMap<String, Order> orders = new HashMap<String, Order>();

	private AID [] prooferAgents;
	private AID [] ovenAgents;
	private AID [] bakingPreparationAgents; // similar to the preparationTableAgents in the Dough Stage
	private AID [] coolingRackAgents;

	private AtomicBoolean coolingRequested = new AtomicBoolean(false);

	private WorkQueue needsBaking;
	private WorkQueue needsPreparation;
	private WorkQueue needsCooling;
	private static final String NEEDS_BAKING = "needsBaking";
	private static final String NEEDS_PREPARATION = "needsPreparation";
	private static final String NEEDS_COOLING = "needsCooling";
	private int coolingRequestCounter = 0;


	protected void setup() {
		super.setup();
		System.out.println(getAID().getLocalName() + " is ready.");

		// Load bakery information (includes recipes for each product)
		getbakery();

		// Queue of productStatus which require baking
		needsBaking = new WorkQueue();
		// Queue of productStatus which require preparation (twisting, frying, ...)
		needsPreparation = new WorkQueue();
		// Queue of productStatus which require cooling
		needsCooling = new WorkQueue();

		// Register the Baking-manager in the yellow pages
		this.register("Baking-manager", "JADE-bakery");

		this.getProoferAIDs();
		this.getOvenAIDs();
		this.getBakingPreparationAIDs();
		this.getCoolingRackAIDs();


		// Activate behavior that receives orders
		// addBehaviour(new ReceiveOrders());

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

		addBehaviour(new ReceiveDoughNotification());
		addBehaviour(new ReceiveBakingNotification());
		addBehaviour(new ReceivePreparationNotification());

		// Time tracker behavior
        addBehaviour(new timeTracker());

	}

	protected void takeDown() {
		System.out.println(getAID().getLocalName() + ": Terminating.");
		this.deRegister();
	}

	public void getProoferAIDs() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();

		sd.setType("Proofer");
		template.addServices(sd);
		try {
			DFAgentDescription [] result = DFService.search(this, template);
			System.out.println(getAID().getLocalName() + "Found the following Proofer agents:");
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

	public void getOvenAIDs() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();

		sd.setType("OvenAgent");
		template.addServices(sd);
		try {
			DFAgentDescription [] result = DFService.search(this, template);
			System.out.println(getAID().getLocalName() + "Found the following Oven agents:");
			ovenAgents = new AID [result.length];

			for (int i = 0; i < result.length; ++i) {
				ovenAgents[i] = result[i].getName();
				System.out.println(ovenAgents[i].getName());
			}

		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	public void getCoolingRackAIDs() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();

		sd.setType("CoolingRack");
		template.addServices(sd);
		try {
			DFAgentDescription [] result = DFService.search(this, template);
			System.out.println(getAID().getLocalName() + "Found the following Cooling agents:");
			coolingRackAgents = new AID [result.length];

			for (int i = 0; i < result.length; ++i) {
				coolingRackAgents[i] = result[i].getName();
				System.out.println(coolingRackAgents[i].getName());
			}

		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	public void getBakingPreparationAIDs() {
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();

		sd.setType("BakingPreparation");
		template.addServices(sd);
		try {
			DFAgentDescription [] result = DFService.search(this, template);
			System.out.println(getAID().getLocalName() + "Found the following Baking Preparation agents:");
			bakingPreparationAgents = new AID [result.length];

			for (int i = 0; i < result.length; ++i) {
				bakingPreparationAgents[i] = result[i].getName();
				System.out.println(bakingPreparationAgents[i].getName());
			}

		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }
            // else{
            //     System.out.println("-------> Baking Manager -> " + baseAgent.getCurrentHour());
            // }
            baseAgent.finished();
        }
    }

	public void getbakery(){

		String jsonDir = "src/main/resources/config/shared_stage_communication/";
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

	public void queueBaking(String productType, Vector<String> guids, Vector<Integer> productQuantities ) {
		// Add productStatus to the needsBaking WorkQueue

		for (String guid : guids) {

			int amount = -1;
			String status = NEEDS_BAKING;
			Product product = bakery.findProduct(productType);
			Order order = orders.get(guid);

			for(BakedGood bakedGood : order.getBakedGoods()) {
				if (bakedGood.getName().equals(productType)) {
					amount = bakedGood.getAmount();
				}

			}
			ProductStatus productStatus = new ProductStatus(guid, status, amount, product);
			needsBaking.addProduct(productStatus);
		}
	}

	public void queuePreparation(String productType, Vector<String> guids ) {
		// Add productStatus to the needsPreparation WorkQueue

		for (String guid : guids) {

			int amount = -1;
			String status = NEEDS_PREPARATION;
			Product product = bakery.findProduct(productType);
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

	public void queueCooling(String productType, Vector<String> guids ) {
		// Add productStatus to the needsCooling WorkQueue

		for (String guid : guids) {

			int amount = -1;
			String status = NEEDS_COOLING;
			Product product = bakery.findProduct(productType);
			Order order = orders.get(guid);

			for(BakedGood bakedGood : order.getBakedGoods()) {
				if (bakedGood.getName().equals(productType)) {
					amount = bakedGood.getAmount();
				}

			}
			ProductStatus productStatus = new ProductStatus(guid, status, amount, product);
			needsCooling.addProduct(productStatus);
		}
	}

	public BakingRequest createBakingRequest() {
		// Checks the needsBaking WorkQueue and creates a bakingRequestMessage
		Vector<ProductStatus> products = needsBaking.getProductBatch();

		BakingRequest bakingRequest = null;

		if (products != null) {

			Vector<String> guids = new Vector<String>();
			Vector<Integer> productQuantities = new Vector<Integer>();


			for (ProductStatus productStatus : products) {
				guids.add(productStatus.getGuid());
				productQuantities.add(productStatus.getAmount());
			}

			String productType = products.get(0).getProduct().getGuid();

			int bakingTemp = products.get(0).getProduct().getRecipe().getBakingTemp();
			float bakingTime = products.get(0).getProduct().getRecipe().getActionTime(Step.BAKING_STEP);


			bakingRequest = new BakingRequest(guids, productType, bakingTemp, bakingTime, productQuantities);

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

	public Vector<CoolingRequest> createCoolingRequests() {
		// Checks the needsCooling WorkQueue and creates a coolingRequestMessage
		Vector<ProductStatus> products = needsCooling.getProductBatch();
		Vector<CoolingRequest> coolingRequests = new Vector<CoolingRequest>();


		if (products != null) {

			Vector<String> guids = new Vector<String>();
			Vector<Integer> productQuantities = new Vector<Integer>();



			for (ProductStatus productStatus : products) {

				String productName = productStatus.getProduct().getGuid();

				float coolingTime = productStatus.getProduct().getRecipe().getActionTime(Step.COOLING_STEP);

				int boxingTemp = productStatus.getProduct().getPackaging().getBoxingTemp();

				int quantity = productStatus.getAmount();

				CoolingRequest coolingRequest = new CoolingRequest(productName, coolingTime, quantity);
				// System.out.println("-------> HERE");
				coolingRequests.add(coolingRequest);
			}


		}

		return coolingRequests;

	}


	/* This is the behavior used for receiving doughNotifications */
	private class ReceiveDoughNotification extends CyclicBehaviour {
		public void action() {
			// baseAgent.finished(); //call it if there are no generic behaviours
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
					MessageTemplate.MatchConversationId("dough-notification"));

			ACLMessage msg = baseAgent.receive(mt);

			if (msg != null) {
				System.out.println(getAID().getLocalName()+" Received dough Notification from " + msg.getSender());
				String doughNotificationString = msg.getContent();
				System.out.println("Dough notification contains -> " +doughNotificationString);

				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.CONFIRM);
				reply.setContent("Dough Notification was received");
				reply.setConversationId("dough-notification-reply");
				baseAgent.sendMessage(reply);


				DoughNotification doughNotification = JSONConverter.parseDoughNotification(doughNotificationString);
				String productType = doughNotification.getProductType();
				Vector<String> guids = doughNotification.getGuids();
				Vector<Integer> productQuantities = doughNotification.getProductQuantities();

				//Add the new request to the needsBaking workqueue
				queueBaking(productType, guids, productQuantities);
				// Create bakingRequest
				BakingRequest bakingRequest = createBakingRequest();

				// Convert bakingRequest to String
				Gson gson = new Gson();
				String bakingRequestString = gson.toJson(bakingRequest);

				// Send bakingRequestMessage
				// System.out.println("Requesting baking");
				addBehaviour(new RequestBaking(bakingRequestString));

			}
			else {
				block();
			}
		}
	}

	/* This is the behaviour used for receiving baking notification */
	private class ReceiveBakingNotification extends CyclicBehaviour {
		public void action() {
			// baseAgent.finished(); //call it if there are no generic behaviours

			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
					MessageTemplate.MatchConversationId("baking-notification"));

			ACLMessage msg = baseAgent.receive(mt);

			if (msg != null) {

				System.out.println("-------> " + getAID().getLocalName()+" Received Baking Notification from " + msg.getSender());
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
				queuePreparation(productType, guids);

				// Create preparationRequestMessage with the information in the queuePreparation
				PreparationRequest preparationRequestMessage = createPreparationRequestMessage();

				// Convert preparationRequestMessage to String
				Gson gson = new Gson();

				String preparationRequestString = gson.toJson(preparationRequestMessage);

				// Send preparationRequestMessage
				addBehaviour(new RequestPreparation(preparationRequestString));

			}

			else {
				block();
			}
		}
	}

	/* This is the behavior used for receiving preparation notifications */
	private class ReceivePreparationNotification extends CyclicBehaviour {
		public void action() {
			// baseAgent.finished(); //call it if there are no generic behaviours

			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
					MessageTemplate.MatchConversationId("preparationBaking-notification"));

			ACLMessage msg = baseAgent.receive(mt);

			if (msg != null) {

				System.out.println("-------> " + getAID().getLocalName()+" Received Baking Preparation Notification from " + msg.getSender());
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

				// TODO: Check if there are products in the needsBaking workqueue and send a bakingRequest accordingly

				// Add guids with this productType to the queueCooling
				queueCooling(productType, guids);

				//Create coollingRequestMessages with the information in the queueCooling
				Vector<CoolingRequest> coolingRequests = createCoolingRequests();

				Gson gson = new Gson();

				for (CoolingRequest coolingRequest : coolingRequests) {
					String coolingRequestString = gson.toJson(coolingRequest);
					// Adds one behaviour per coolingRequest
					addBehaviour(new RequestCooling(coolingRequestString, coolingRequestCounter));
					coolingRequestCounter ++;

				}
			}
			else {
				block();
			}
		}
	}

	//This is the behaviour used for sending a BakingRequest
	private class RequestBaking extends Behaviour{
		private String bakingRequest;
		private MessageTemplate mt;
		private int option = 0;

		public RequestBaking(String bakingRequest){
			this.bakingRequest = bakingRequest;
		}
		public void action(){
			//blocking action
			if (!baseAgent.getAllowAction()) {
				return;
			}
			switch(option){
				case 0:

					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent(bakingRequest);
					msg.setConversationId("baking-request");

					// Send kneadingRequest msg to all kneadingMachineAgents
					for (int i=0; i<ovenAgents.length; i++){
						msg.addReceiver(ovenAgents[i]);
					}
					// msg.setReplyWith("msg"+System.currentTimeMillis());
					baseAgent.sendMessage(msg);  // calling sendMessage instead of send

					option = 1;
					System.out.println(getLocalName()+" Sent bakingRequest" + bakingRequest);
					break;

				case 1:
						mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
	                        MessageTemplate.MatchConversationId("baking-request-reply"));
						ACLMessage reply = baseAgent.receive(mt);

	                    if (reply != null) {
	                        System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
	                        option = 2;
	                    }
	                    else {
	                        block();
	                    }
	                    break;
			default:
				break;
			}
		}
		public boolean done(){
			if (option == 2) {
                return true;
            }
			return false;
		}
	}

	//This is the behaviour used for sending a PreparationRequest
	private class RequestPreparation extends Behaviour{
		private String preparationRequest;
		private MessageTemplate mt;
		private ACLMessage msg;
		private int option = 0;

		public RequestPreparation(String preparationRequest){
			this.preparationRequest = preparationRequest;
		}
		public void action(){
			//blocking action
			// if (!baseAgent.getAllowAction()) {
			// 	return;
			// }
			switch(option){
				case 0:
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent(preparationRequest);
					msg.setConversationId("preparationBaking-request");

					// Send preparationRequest msg to all preparationTableAgents
					for (int i=0; i<bakingPreparationAgents.length; i++){
						msg.addReceiver(bakingPreparationAgents[i]);
					}
					// msg.setReplyWith("msg"+System.currentTimeMillis());
					baseAgent.sendMessage(msg);  // calling sendMessage instead of send

					option = 1;
					System.out.println(getLocalName()+" Sent baking preparationRequest" + preparationRequest);
					break;

				case 1:
					mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("preparationBaking-request-reply"));
					ACLMessage reply = baseAgent.receive(mt);


					if (reply != null) {
                        System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                        option = 2;
                    }
                    else {
                        block();
                    }
                    break;

				default:
					break;
			}
		}
		public boolean done(){
			if (option == 2){
				// baseAgent.finished();
				return true;
			}
			return false;
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
			//blocking action
			// if (!baseAgent.getAllowAction()) {
			// 	return;
			// }
			switch(option){
				case 0:

					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent(coolingRequest);
					msg.setConversationId("cooling-request");

					// Send kneadingRequest msg to all kneadingMachineAgents
					for (int i=0; i<coolingRackAgents.length; i++){
						msg.addReceiver(coolingRackAgents[i]);
					}
					// msg.setReplyWith("msg"+System.currentTimeMillis());
					baseAgent.sendMessage(msg);  // calling sendMessage instead of send

					option = 1;
					System.out.println(getLocalName()+" Sent coolingRequest" + coolingRequest);
					break;

				case 1:
					mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("cooling-request-reply"));

                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {
                        System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                        option = 2;
                    }
                    else {
                        block();
                    }
                    break;

			default:
				break;
			}
		}
		public boolean done(){
			if (option == 2){
				// baseAgent.finished();
				return true;

			}
			return false;
		}
	}



}
