package org.mas_maas.agents;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.maas.JSONConverter;
import org.maas.messages.PreparationNotification;
import org.maas.messages.PreparationRequest;
import org.maas.utils.Time;
import org.maas.Objects.Step;

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

public class BakingPreparationAgent extends BaseAgent {
    private AID bakingManagerAgent;

    private AtomicBoolean preparationInProcess = new AtomicBoolean(false);
    private AtomicBoolean fullPrepDone = new AtomicBoolean(false);
    private AtomicInteger stepCounter = new AtomicInteger(0);
    private AtomicInteger messageProcessing = new AtomicInteger(0);
    private AtomicBoolean isInProductionTime = new AtomicBoolean (false);

    private Vector<String> guids;
    private Vector<Integer> productQuantities;
    private String productType;
    private Vector<Step> steps;

    private Float stepDuration;
    private int curStepIndex = 0;
    private String stepAction;
    private int productIndex = 0;
    private int totalQuantity = 0;

    private String bakingPreparationAgentName;
    private String bakingManagerName;

    private boolean isAvailable = true;

    protected void setup() {
        super.setup();

        Object[] args = getArguments();

        if(args != null && args.length > 0){
            this.bakingPreparationAgentName = (String) args[0];
            this.bakingManagerName = (String) args[1];
        }

        this.getBakingManagerAID();

        System.out.println("Hello! " + getAID().getLocalName() + " is ready." + "its bakingManager is: " + bakingManagerAgent.getName());
        this.register(bakingPreparationAgentName, "JADE-bakery");


        // Time tracker behavior
        addBehaviour(new timeTracker());

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceiveProposalRequests());
        addBehaviour(new ReceivePreparationRequests());
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
            }else{

                if (preparationInProcess.get() && isInProductionTime.get()){
                    int curCount = stepCounter.incrementAndGet();
                    System.out.println(">>>>> BakingPrep Counter -> " + getAID().getLocalName() + " " + stepCounter + " <<<<<");
                    addBehaviour(new Preparation());
                }
            }

            if (messageProcessing.get() <= 0){
                // Production time is from midnight to lunch (from 00.00 hrs to 12 hrs)
               if ((baseAgent.getCurrentTime().greaterThan(new Time(baseAgent.getCurrentDay(), 0, 0)) ||

                       baseAgent.getCurrentTime().equals(new Time(baseAgent.getCurrentDay(), 0, 0))) &&

                       baseAgent.getCurrentTime().lessThan(new Time(baseAgent.getCurrentDay(), 12, 0)))
                {
                   isInProductionTime.set(true);

               }else{
                   isInProductionTime.set(false);
               }
            }
            baseAgent.finished();
        }
    }

    private class ReceiveProposalRequests extends CyclicBehaviour{
        public void action(){
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("baking-preparation-request"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null){
                String content = msg.getContent();
                // System.out.println(getAID().getLocalName() + "has received a proposal request from " + msg.getSender().getName());

                ACLMessage reply = msg.createReply();

                if (isAvailable){
                	// System.out.println(getAID().getLocalName() + " is available");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Hey I am free, do you wanna use me ;)?" + content);
                }else{
                	// System.out.println(getAID().getLocalName() + " is unavailable");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Sorry, I am married potato :c" + content);
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

    // Receiving Preparation requests behaviour
    private class ReceivePreparationRequests extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if (!isAvailable){

                    // System.out.println(getAID().getLocalName()  + " is already taken");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("bakingPrepTable is taken");
                    // System.out.println(getAID().getLocalName() + " failed preparation of " + msg.getContent());
                }
                else{
                    isAvailable = false;

                    String content = msg.getContent();
                    System.out.println(getAID().getLocalName() + " WILL perform baking preparation for \n \t"
                                + msg.getSender().getLocalName() + ": " + content);

                    PreparationRequest preparationRequest = JSONConverter.parsePreparationRequest(content);

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Preparation request was received " + content);

                    guids = preparationRequest.getGuids();
                    productType = preparationRequest.getProductType();
                    steps = preparationRequest.getSteps();
                    productQuantities = preparationRequest.getProductQuantities();
                    System.out.println(" -----> old totalQuantity " + totalQuantity);
                    for (Integer quantity : productQuantities){
                        totalQuantity = totalQuantity + quantity;
                    }
                    System.out.println("bakingPreparation table quantities " + totalQuantity);
                    // System.out.println(getAID().getLocalName() + " WILL do the following actions " + steps);

                    addBehaviour(new Preparation());
                }
                baseAgent.sendMessage(reply);
                messageProcessing.decrementAndGet();

            }else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    // performs Preparation process
    private class Preparation extends OneShotBehaviour {
        public void action(){
            // TODO: Iterate over different guids
            if (!preparationInProcess.get()){
                preparationInProcess.set(true);

                if (curStepIndex < steps.size()){

                    stepAction = steps.get(curStepIndex).getAction();

                    stepDuration = steps.get(curStepIndex).getDuration();
                    // }
                    System.out.println("Performing baking " + stepAction + " for " + stepDuration
                                      + " for " + totalQuantity + " "+ productType);
                }else{
                    curStepIndex = 0;
                    stepCounter.set(0);
                    preparationInProcess.set(false);
                    isAvailable = true;
                    totalQuantity = 0;
                    addBehaviour(new SendPreparationNotification());
                    // this.done();
                }
            }

            if (stepCounter.get() >= stepDuration){
                curStepIndex++;
                stepCounter.set(0);
                preparationInProcess.set(false);
                addBehaviour(new Preparation());
            }

        }
    }



    // Send a preparationNotification msg to the doughManager agents
    private class SendPreparationNotification extends Behaviour {

        private AID [] bakingManagerAgents;
        private MessageTemplate mt;
        private int option = 0;
        private Gson gson = new Gson();
        private PreparationNotification preparationNotification = new PreparationNotification(guids,productType);
        private String preparationNotificationString = gson.toJson(preparationNotification);

        public void action() {
            messageProcessing.getAndIncrement();
            switch (option) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(preparationNotificationString);
                    msg.setConversationId("preparationBaking-notification");
                    msg.addReceiver(bakingManagerAgent);

                    // msg.setReplyWith("msg" + System.currentTimeMillis());
                    baseAgent.sendMessage(msg);

                    option = 1;
                    System.out.println(getAID().getLocalName() + " Sent baking preparationNotification of " + productType );
                    break;

                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                            MessageTemplate.MatchConversationId("preparationBaking-notification-reply"));

                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {
                        System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
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
