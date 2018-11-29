package org.mas_maas.agents;

import java.util.Vector;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.PreparationNotification;
import org.mas_maas.messages.PreparationRequest;

import org.mas_maas.objects.Step;
import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.DoughPrepTable;
import org.mas_maas.objects.Equipment;

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

public class PreparationTableAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private AtomicBoolean preparationInProcess = new AtomicBoolean(false);
    private AtomicBoolean fullPrepDone = new AtomicBoolean(false);

    private Bakery bakery;
    private Vector<DoughPrepTable> preparationTables = new Vector<DoughPrepTable> ();
    private Vector<Equipment> equipment;

    private Vector<String> guids;
    private Vector<Integer> productQuantities;
    private String productType;
    private Vector<Step> steps;
    private int stepCounter;

    protected void setup() {
        super.setup();

        System.out.println(getAID().getLocalName() + " is ready.");

        this.register("Preparation-table", "JADE-bakery");

        this.getDoughManagerAIDs();

        // Load bakery information (includes recipes for each product)
        getbakery();

        // Get KneadingMachines
        this.getPreparationTables();

        stepCounter = 0;
        // Time tracker behavior
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
    }

    public void getPreparationTables(){
        equipment = bakery.getEquipment();
        System.out.println("Bakery name " + bakery.getName());

        for (int i = 0; i < equipment.size(); i++){
            if (equipment.get(i) instanceof DoughPrepTable){
                preparationTables.add((DoughPrepTable) equipment.get(i));
            }
        }

        System.out.println("Preparation tables found " + preparationTables.size());

    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }else{
                if (preparationInProcess.get() && !fullPrepDone.get()){
                    stepCounter++;
                    System.out.println("-------> Dough Prep Clock-> " + baseAgent.getCurrentHour());
                    System.out.println("-------> step Counter -> " + stepCounter);
                }
            }
            baseAgent.finished();
        }
    }

    // Receiving Preparation requests behaviour
    private class ReceivePreparationRequests extends CyclicBehaviour {
        public void action() {

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
        private String guidTableAvailable;
        private String stepAction;
        private int guidIdx = 0;

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

        public void action(){
            // TODO: Iterate over different guids
            if (!preparationInProcess.get() && !fullPrepDone.get()){
                guidTableAvailable = findAvailableTables();
                if (guidTableAvailable != "NOT_AVAILABLE"){
                    System.out.println("----> Using dough prepTable " + guidTableAvailable);
                }else{
                    System.out.println("----> No dough prepTable currently available");
                    //TODO: What do we do?
                }
                if (stepIdx < steps.size()){
                    stepAction = steps.get(stepIdx).getAction();
                    if (stepAction.equals(Step.ITEM_PREPARATION_STEP)){
                        stepDuration = steps.get(stepIdx).getDuration() * productQuantities.get(guidIdx);
                    }else{
                        stepDuration = steps.get(stepIdx).getDuration();
                    }
                    System.out.println("-----> " + guidTableAvailable + " Performing " + stepAction);
                    System.out.println("-----> Preparation for " + stepDuration);
                }else{
                    fullPrepDone.set(true);
                    stepIdx = 0;
                    addBehaviour(new SendPreparationNotification(doughManagerAgents));
                    // this.done();
                }
            }

            if (stepCounter >= stepDuration && !fullPrepDone.get()){
                preparationInProcess.set(false);
                releaseUsedTable(guidTableAvailable);
                stepIdx++;
                stepCounter = 0;
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
               myAgent.doDelete();
               return true;
           }

           return false;
       }
   }

}
