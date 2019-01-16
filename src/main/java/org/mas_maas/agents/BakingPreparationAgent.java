package org.mas_maas.agents;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.maas.JSONConverter;
import org.maas.messages.PreparationNotification;
import org.maas.messages.PreparationRequest;
import org.maas.Objects.Step;

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

public class BakingPreparationAgent extends BaseAgent {
    private AID [] bakingManagerAgents;

    private AtomicBoolean preparationInProcess = new AtomicBoolean(false);
    private AtomicBoolean fullPrepDone = new AtomicBoolean(false);
    private AtomicInteger stepCounter = new AtomicInteger(0);

    private Vector<String> guids;
    private Vector<Integer> productQuantities;
    private String productType;
    private Vector<Step> steps;

    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("BakingPreparation", "JADE-bakery");
        this.getBakingManagerAIDs();

        // Time tracker behavior
        addBehaviour(new timeTracker());

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceivePreparationRequests());
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

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }else{
                if (preparationInProcess.get() && !fullPrepDone.get()){
                    int curStepCount = stepCounter.incrementAndGet();
                    System.out.println("-------> Baking Prep Clock-> " + baseAgent.getCurrentHour());
                    System.out.println("-------> step Counter -> " + curStepCount);
                }
            }
            baseAgent.finished();
        }
    }

    // Receiving Preparation requests behaviour
    private class ReceivePreparationRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("preparationBaking-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                System.out.println(getAID().getLocalName() + " Received baking preparation request from " + msg.getSender());
                String content = msg.getContent();
                System.out.println("Preparation request contains -> " + content);
                PreparationRequest preparationRequest = JSONConverter.parsePreparationRequest(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Preparation request was received");
                reply.setConversationId("preparationBaking-request-reply");
                baseAgent.sendMessage(reply);

                guids = preparationRequest.getGuids();
                productType = preparationRequest.getProductType();
                steps = preparationRequest.getSteps();
                System.out.println("==================================================");
                System.out.println("----> I should do the following actions " + steps);
                System.out.println("==================================================");
                productQuantities = preparationRequest.getProductQuantities();
                addBehaviour(new Preparation());
            }
            else {
                block();
            }
        }
    }

    // performs Preparation process
    private class Preparation extends Behaviour {
        private Float stepDuration;
        private int stepIdx = 0;
        private int guidIdx = 0;
        private String stepAction;

        public void action(){
            // TODO: Iterate over different guids
            if (!preparationInProcess.get() && !fullPrepDone.get()){
                preparationInProcess.set(true);
                if (stepIdx < steps.size()){
                    stepAction = steps.get(stepIdx).getAction();
                    // if (stepAction.equals(Step.ITEM_PREPARATION_STEP)){
                    //     stepDuration = steps.get(stepIdx).getDuration() * productQuantities.get(guidIdx);
                    // }else{
                    stepDuration = steps.get(stepIdx).getDuration();
                    // }
                    System.out.println("-----> Performing " + stepAction);
                    System.out.println("-----> Preparation for " + stepDuration);
                }else{
                    fullPrepDone.set(true);
                    stepIdx = 0;
                    addBehaviour(new SendPreparationNotification(bakingManagerAgents));
                    // this.done();
                }
            }

            if (stepCounter.get() >= stepDuration && !fullPrepDone.get()){
                preparationInProcess.set(false);
                stepIdx++;
                stepCounter.set(0);
            }

        }
        public boolean done(){
            if (fullPrepDone.get()){
                fullPrepDone.set(false);
                return true;
            }{
                return false;
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

    public SendPreparationNotification(AID [] bakingManagerAgents){
        this.bakingManagerAgents = bakingManagerAgents;
    }

       public void action() {

           switch (option) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(preparationNotificationString);
                    msg.setConversationId("preparationBaking-notification");

                    // Send preparationNotification msg to bakingManagerAgents
                    for (int i = 0; i < bakingManagerAgents.length; i++){
                        msg.addReceiver(bakingManagerAgents[i]);
                    }
                    // msg.setReplyWith("msg" + System.currentTimeMillis());
                    baseAgent.sendMessage(msg);

                    option = 1;
                    System.out.println(getAID().getLocalName() + " Sent baking preparationNotification");
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
                        block();
                    }
                    break;

                default:
                    break;
           }
       }

       public boolean done() {
           if (option == 2) {
               baseAgent.finished(); // calling finished method
               myAgent.doDelete();
               return true;
           }

           return false;
       }
   }

}
