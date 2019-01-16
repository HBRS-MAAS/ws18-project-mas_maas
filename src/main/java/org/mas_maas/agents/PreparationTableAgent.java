package org.mas_maas.agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.maas.JSONConverter;
import org.maas.messages.PreparationNotification;
import org.maas.messages.PreparationRequest;
import org.maas.Objects.Bakery;
import org.maas.Objects.DoughPrepTable;
import org.maas.Objects.Equipment;
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

public class PreparationTableAgent extends BaseAgent {
    private AID doughManagerAgent;

    //private AtomicBoolean processingMessage = new AtomicBoolean(false);
    private AtomicBoolean preparationInProcess = new AtomicBoolean(false);
    private AtomicBoolean fullPrepDone = new AtomicBoolean(false);
    private AtomicInteger stepCounter = new AtomicInteger(0);

    private AtomicInteger messageProcessing = new AtomicInteger(0);

    private DoughPrepTable doughPrepTable;
    private String doughPrepTableName;
    private String doughManagerName;

    private Vector<String> guids;
    private Vector<Integer> productQuantities;
    private String productType;
    private Vector<Step> steps;

    private Float stepDuration;
    private int curStepIndex = 0;
    private String stepAction;
    private int productIndex = 0;

    protected void setup() {
        super.setup();

        Object[] args = getArguments();

        if(args != null && args.length > 0){
            this.doughPrepTable= (DoughPrepTable) args[0];
            this.doughPrepTableName = (String) args[1];
            this.doughManagerName = (String) args[2];
        }

        this.getDoughManagerAID();

        System.out.println("Hello! " + getAID().getLocalName() + " is ready." + "its DougManager is: " + doughManagerAgent.getName());

        this.register(this.doughPrepTableName, "JADE-bakery");

        doughPrepTable.setAvailable(true);


        stepCounter.set(0);

        // Time tracker behavior
        addBehaviour(new timeTracker());

        addBehaviour(new ReceiveProposalRequests());
        addBehaviour(new ReceivePreparationRequests());
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getDoughManagerAID() {
        doughManagerAgent = new AID (doughManagerName, AID.ISLOCALNAME);

    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }else{
                // if (preparationInProcess.get() && !fullPrepDone.get()){
                if (preparationInProcess.get()){
                    int curCount = stepCounter.incrementAndGet();
                    System.out.println(">>>>> DoughPrep Counter -> " + getAID().getLocalName() + " " + stepCounter + " <<<<<");
                    addBehaviour(new Preparation());
                }
            }
            if (messageProcessing.get() <= 0)
            {
                baseAgent.finished();
            }
        }
    }

    private class ReceiveProposalRequests extends CyclicBehaviour{
        public void action(){
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("preparation-request")); //MessageTemplate.MatchConversationId("preparation-request"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null){
                String content = msg.getContent();
                // System.out.println(getAID().getLocalName() + "has received a proposal request from " + msg.getSender().getName());

                ACLMessage reply = msg.createReply();
                if (doughPrepTable.isAvailable()){
                	// System.out.println(getAID().getLocalName() + " is available");
                    // fullPrepDone.set(false);
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
                // MessageTemplate.MatchConversationId("preparation-request"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                ACLMessage reply = msg.createReply();

                if (!doughPrepTable.isAvailable()){

                    // System.out.println(getAID().getLocalName()  + " is already taken");
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("doughPrepTable is taken");
                    //reply.setConversationId("preparation-request");
                    //baseAgent.sendMessage(reply);
                    // System.out.println(getAID().getLocalName() + " failed preparation of " + msg.getContent());
                }
                else{
                    doughPrepTable.setAvailable(false);
                    String content = msg.getContent();
                    System.out.println("***** > " + getAID().getLocalName() + " WILL perform preparation for " + msg.getSender() + "Preparation information -> " + content);

                    PreparationRequest preparationRequest = JSONConverter.parsePreparationRequest(content);

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Preparation request was received " + content);
                    //reply.setConversationId("preparation-request");
                    //baseAgent.sendMessage(reply);

                    guids = preparationRequest.getGuids();
                    productType = preparationRequest.getProductType();
                    steps = preparationRequest.getSteps();
                    productQuantities = preparationRequest.getProductQuantities();

                    System.out.println(getAID().getLocalName() + " WILL do the following actions " + steps);

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
            // if (!preparationInProcess.get() && !fullPrepDone.get()){
            if (!preparationInProcess.get()){

                preparationInProcess.set(true);
                doughPrepTable.setAvailable(false);

                if (curStepIndex < steps.size()){
                    // Get the action and its duration

                    stepAction = steps.get(curStepIndex).getAction();

                    if (stepAction.equals(Step.ITEM_PREPARATION_STEP)){
                        stepDuration = steps.get(curStepIndex).getDuration() * productQuantities.get(productIndex);
                    }else{
                        stepDuration = steps.get(curStepIndex).getDuration();
                    }

                    System.out.println("-----> Performing " + stepAction + " for " + stepDuration
                                      + " for product " + productType);

                }else{
                    // We have performed all preparation actions.
                    curStepIndex = 0;
                    // fullPrepDone.set(true);
                    stepCounter.set(0);
                    preparationInProcess.set(false);
                    doughPrepTable.setAvailable(true);
                    addBehaviour(new SendPreparationNotification());
                }
            }

            // if (stepCounter.get() >= stepDuration && !fullPrepDone.get()){
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
    private MessageTemplate mt;
    private int option = 0;
    private Gson gson = new Gson();
    private PreparationNotification preparationNotification = new PreparationNotification(guids,productType);
    private String preparationNotificationString = gson.toJson(preparationNotification);

       public void action() {
           messageProcessing.incrementAndGet();
           switch (option) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(preparationNotificationString);
                    msg.setConversationId("preparation-notification");

                    msg.addReceiver(doughManagerAgent);

                    baseAgent.sendMessage(msg);

                    System.out.println(getAID().getLocalName() + " Sent preparationNotification to" + doughManagerAgent);
                    messageProcessing.decrementAndGet();
                    option = 1;
                    break;

                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("preparation-notification-reply"));

                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {
                        System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                        option = 2;
                    }
                    else {
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
           if (option == 2) {
               return true;
           }

           return false;
       }
   }

}
