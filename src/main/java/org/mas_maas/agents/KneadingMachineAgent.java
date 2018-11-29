package org.mas_maas.agents;

import java.util.Vector;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.KneadingNotification;
import org.mas_maas.messages.KneadingRequest;

import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.KneadingMachine;
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
import java.util.concurrent.atomic.AtomicBoolean;
public class KneadingMachineAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private AtomicBoolean kneadingInProcess = new AtomicBoolean(false);

    private Bakery bakery;
    private Vector<KneadingMachine> kneadingMachines = new Vector<KneadingMachine> ();
    private Vector<Equipment> equipment;

    private Vector<String> guids;
    private String productType;

    private int kneadingCounter;

    protected void setup() {
        super.setup();

        System.out.println(getAID().getLocalName() + " is ready.");

        // Register KneadingMachine Agent to the yellow Pages
        this.register("Kneading-machine", "JADE-bakery");

        // Get Agents AIDS
        this.getDoughManagerAIDs();

        // Load bakery information (includes recipes for each product)
        getbakery();

        // Get KneadingMachines
        this.getKneadingMachines();

        kneadingCounter = 0;
        // Time tracker behavior
        addBehaviour(new timeTracker());
        // Creating receive kneading requests behaviour
        addBehaviour(new ReceiveKneadingRequests());


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

    public void getKneadingMachines(){
        equipment = bakery.getEquipment();
        System.out.println("Bakery name " + bakery.getName());

        for (int i = 0; i < equipment.size(); i++){
            if (equipment.get(i) instanceof KneadingMachine){
                // System.out.println("Kneading machines found " + equipment.get(i));
                kneadingMachines.add( (KneadingMachine) equipment.get(i));
            }
        }

        System.out.println("Kneading machines found " + kneadingMachines.size());

    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }else{
                if (kneadingInProcess.get()){
                    kneadingCounter++;
                    System.out.println("-------> Kneading Clock-> " + baseAgent.getCurrentHour());
                    System.out.println("-------> Kneading Counter -> " + kneadingCounter);
                }
            }
            baseAgent.finished();
        }
    }

    // Receiving Kneading requests behavior
    private class ReceiveKneadingRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("kneading-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {

                System.out.println(getAID().getLocalName() + " received kneading requests from " + msg.getSender());
                String content = msg.getContent();
                System.out.println("Kneading request contains -> " + content);
                KneadingRequest kneadingRequest = JSONConverter.parseKneadingRequest(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Kneading request was received");
                reply.setConversationId("kneading-request-reply");
                baseAgent.sendMessage(reply);

                Float kneadingTime = kneadingRequest.getKneadingTime();
                guids = kneadingRequest.getGuids();
                productType = kneadingRequest.getProductType();

                addBehaviour(new Kneading(kneadingTime));

            }else {
                block();
            }
        }
    }

    // performs Kneading process
    private class Kneading extends Behaviour {
        private Float kneadingTime;
        private int option = 0;
        private String guidAvailable;

        public Kneading(Float kneadingTime){
            this.kneadingTime = kneadingTime;
            System.out.println("----> "+ getAID().getLocalName() + " Kneading for " + kneadingTime);
        }

        public String findAvailableMachines(){
            for (KneadingMachine kneadingMachine : kneadingMachines) {
                if (kneadingMachine.isAvailable()){
                    kneadingMachine.setAvailable(false);
                    kneadingInProcess.set(true);
                    return kneadingMachine.getGuid();
                }
            }
            return "NOT_AVAILABLE";
        }

        public void releaseUsedMachine(String guid){
            for (KneadingMachine kneadingMachine : kneadingMachines) {
                if (kneadingMachine.getGuid().equals(guid)){
                    kneadingMachine.setAvailable(true);
                }
            }
        }

        public void action(){
            // System.out.println("-------> I am alive!");
            if (kneadingCounter < kneadingTime){
                if (!kneadingInProcess.get()){
                    guidAvailable = findAvailableMachines();
                    if (guidAvailable != "NOT_AVAILABLE"){
                        System.out.println("----> Using kneading machine " + guidAvailable);
                    }else{
                        System.out.println("----> No kneading machine currently available");
                    }
                }

            }else{
                kneadingInProcess.set(false);
                releaseUsedMachine(guidAvailable);
                kneadingCounter = 0;
                addBehaviour(new SendKneadingNotification());
                // this.done();
            }
        }
        public boolean done(){
            if (kneadingInProcess.get()){
                return false;
            }else{
                return true;
            }
            // baseAgent.finished();
        }
    }

    // Send a kneadingNotification msg to the doughManager agents
    private class SendKneadingNotification extends Behaviour {
        private MessageTemplate mt;
        private int option = 0;
        private Gson gson = new Gson();
        private KneadingNotification kneadingNotification = new KneadingNotification(guids, productType);
        private String kneadingNotificationString = gson.toJson(kneadingNotification);

        public void action() {
            switch (option) {
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    // System.out.println("-----> Kneading notification string " + kneadingNotificationString);
                    msg.setContent(kneadingNotificationString);
                    msg.setConversationId("kneading-notification");

                    // Send kneadingNotification msg to doughManagerAgents
                    for (int i = 0; i < doughManagerAgents.length; i++){
                        msg.addReceiver(doughManagerAgents[i]);
                    }

                    baseAgent.sendMessage(msg);

                    option = 1;
                    System.out.println(getAID().getLocalName() + " Sent kneadingNotification");
                    break;




                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("kneading-notification-reply"));

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
                System.out.println(getAID().getLocalName() + " My life is over ");
                baseAgent.finished();
                myAgent.doDelete();
                return true;
            }

           return false;
       }
    }

}
