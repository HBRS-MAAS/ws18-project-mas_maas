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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.maas.agents.BaseAgent;

public class PreparationTableAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private AtomicBoolean processingMessage = new AtomicBoolean(false);
    private AtomicBoolean preparationInProcess = new AtomicBoolean(false);
    private AtomicBoolean fullPrepDone = new AtomicBoolean(false);
    private AtomicInteger stepCounter = new AtomicInteger(0);

    // private Bakery bakery;
    // private Vector<DoughPrepTable> preparationTables = new Vector<DoughPrepTable> ();
    // private Vector<Equipment> equipment;
    private DoughPrepTable doughPrepTable;
    private String doughPrepTableName;
    private String doughManagerName;

    private Vector<String> guids;
    private Vector<Integer> productQuantities;
    private String productType;
    private Vector<Step> steps;

    protected void setup() {
        super.setup();

        Object[] args = getArguments();

        if(args != null && args.length > 0){
            this.doughPrepTable= (DoughPrepTable) args[0];
            this.doughPrepTableName = (String) args[1];
            this.doughManagerName = (String) args[2];
        }

        System.out.println(getAID().getLocalName() + " is ready." + "ITS DougManager is: " + doughManagerName);

        this.register(this.doughPrepTableName, "JADE-bakery");

        //this.getDoughManagerAIDs();

        // Load bakery information (includes recipes for each product)
        //getbakery();

        // Get KneadingMachines
        //this.getPreparationTables();

        // Time tracker behavior
        stepCounter.set(0);
        addBehaviour(new timeTracker());

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceivePreparationRequests());
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getDoughManagerAIDs() {
        /*
        Object the AID of all the dough-manager agents found
        */
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Dough-manager");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Dough-manager agents:");
            doughManagerAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                doughManagerAgents[i] = result[i].getName();
                System.out.println(doughManagerAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // public void getbakery(){
    //
    //     String jsonDir = "src/main/resources/config/shared_stage_communication/";
    //     try {
    //         // System.out.println("Working Directory = " + System.getProperty("user.dir"));
    //         String bakeryFile = new Scanner(new File(jsonDir + "bakery.json")).useDelimiter("\\Z").next();
    //         Vector<Bakery> bakeries = JSONConverter.parseBakeries(bakeryFile);
    //         for (Bakery bakery : bakeries)
    //         {
    //             this.bakery = bakery;
    //         }
    //     } catch (FileNotFoundException e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     }
    // }

    // public void getPreparationTables(){
    //     equipment = bakery.getEquipment();
    //     System.out.println("Bakery name " + bakery.getName());
    //
    //     for (int i = 0; i < equipment.size(); i++){
    //         if (equipment.get(i) instanceof DoughPrepTable){
    //             preparationTables.add((DoughPrepTable) equipment.get(i));
    //         }
    //     }
    //
    //     System.out.println("Preparation tables found " + preparationTables.size());
    //
    // }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            // first we make sure we are even allowed to do anything
            if (!baseAgent.getAllowAction()) {
                return;
            }

            // once we know our agent is able to do an action check if we need to actually do anything
            if (!processingMessage.get())
            {
                if (preparationInProcess.get() && !fullPrepDone.get()){
                    int curCount = stepCounter.incrementAndGet();
                    //System.out.println("-------> Dough Prep Clock-> " + baseAgent.getCurrentHour());
                    //System.out.println("-------> step Counter -> " + curCount);
                }
                baseAgent.finished();
            }
        }
    }

    // Receiving Preparation requests behaviour
    private class ReceivePreparationRequests extends CyclicBehaviour {
        public void action() {
            processingMessage.set(true);

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("preparation-request"));
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                System.out.println(getAID().getLocalName() + " Received preparation request from " + msg.getSender());
                String content = msg.getContent();
                System.out.println("Preparation request contains -> " + content);
                PreparationRequest preparationRequest = JSONConverter.parsePreparationRequest(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Preparation request was received");
                reply.setConversationId("preparation-request-reply");
                baseAgent.sendMessage(reply);

                guids = preparationRequest.getGuids();
                productType = preparationRequest.getProductType();
                steps = preparationRequest.getSteps();
                System.out.println("----> I should do the following actions " + steps);
                productQuantities = preparationRequest.getProductQuantities();

                addBehaviour(new Preparation());
                processingMessage.set(false);
            }
            else {
                processingMessage.set(false);
                block();
            }
        }
    }

    // performs Preparation process
    private class Preparation extends Behaviour {
        private Float stepDuration;
        private int curStepIndex = 0;
        private String guidTableAvailable;
        private String stepAction;
        private int productIndex = 0;

        /*
        public String findAvailableTables(){
            for (DoughPrepTable prepTable : preparationTables) {
                if (prepTable.isAvailable()){
                    prepTable.setAvailable(false);
                    preparationInProcess.set(true);
                    return prepTable.getGuid();
                }
            }
            return "NOT_AVAILABLE";
        }

        public void releaseUsedTable(String guid){
            for (DoughPrepTable prepTable : preparationTables) {
                if (prepTable.getGuid().equals(guid)){
                    prepTable.setAvailable(true);
                }
            }
        }
        */

        public void action(){
            // TODO: Iterate over different guids
            if (!preparationInProcess.get() && !fullPrepDone.get()){
                //guidTableAvailable = findAvailableTables();
                //DoughPrepTable prepTable = preparationTables.get(0); // TODO each agent will only have on table
                preparationInProcess.set(true);
                /* TODO if our one table is busy rejection should happen in the message processing area
                if (guidTableAvailable != "NOT_AVAILABLE"){
                    System.out.println("----> Using dough prepTable " + guidTableAvailable);
                }else{
                    System.out.println("----> No dough prepTable currently available");
                    //TODO: What do we do?
                }
                */

                if (curStepIndex < steps.size()){
                    stepAction = steps.get(curStepIndex).getAction();

                    if (stepAction.equals(Step.ITEM_PREPARATION_STEP)){
                        stepDuration = steps.get(curStepIndex).getDuration() * productQuantities.get(productIndex);
                    }else{
                        stepDuration = steps.get(curStepIndex).getDuration();
                    }

                    System.out.println("-----> Performing " + stepAction);
                    System.out.println("-----> Preparation for " + stepDuration);

                }else{
                    curStepIndex = 0;
                    addBehaviour(new SendPreparationNotification(doughManagerAgents));
                    fullPrepDone.set(true);
                    preparationInProcess.set(false);
                }
            }

            if (stepCounter.get() >= stepDuration && !fullPrepDone.get()){
                //releaseUsedTable(guidTableAvailable);
                curStepIndex++;
                stepCounter.set(0);
                preparationInProcess.set(false);
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
    private AID [] doughManagerAgents;
    private MessageTemplate mt;
    private int option = 0;
    private Gson gson = new Gson();
    private PreparationNotification preparationNotification = new PreparationNotification(guids,productType);
    private String preparationNotificationString = gson.toJson(preparationNotification);

    public SendPreparationNotification(AID [] doughManagerAgents){
        this.doughManagerAgents = doughManagerAgents;
    }

       public void action() {

           switch (option) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(preparationNotificationString);
                    msg.setConversationId("preparation-notification");

                    // Send preparationNotification msg to doughManagerAgents
                    for (int i = 0; i < doughManagerAgents.length; i++){
                        msg.addReceiver(doughManagerAgents[i]);
                    }

                    // msg.setReplyWith("msg" + System.currentTimeMillis());
                    baseAgent.sendMessage(msg);

                    option = 1;
                    System.out.println(getAID().getLocalName() + " Sent preparationNotification");
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
                    break;

                default:
                    break;
           }
       }

       public boolean done() {
           if (option == 2) {
               baseAgent.finished(); // calling finished method
               //myAgent.doDelete();
               return true;
           }

           return false;
       }
   }

}
