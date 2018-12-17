package org.mas_maas.agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.mas_maas.JSONConverter;
import org.mas_maas.messages.KneadingNotification;
import org.mas_maas.messages.KneadingRequest;
// import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.Equipment;
import org.mas_maas.objects.KneadingMachine;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.maas.agents.BaseAgent;
public class KneadingMachineAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private AtomicBoolean kneadingInProcess = new AtomicBoolean(false);
    private AtomicBoolean messageInProcress = new AtomicBoolean(false);
    private AtomicInteger kneadingCounter = new AtomicInteger(0);

    // private Bakery bakery;
    // private Vector<KneadingMachine> kneadingMachines = new Vector<KneadingMachine> ();
    // private Vector<Equipment> equipment;
    private KneadingMachine kneadingMachine;

    private Vector<String> guids;
    private String productType;

    private String kneadingMachineName;
    private String doughManagerName;

    private Float kneadingTime;

    protected void setup() {
        super.setup();

        Object[] args = getArguments();

        if(args != null && args.length > 0){
            System.out.println("Inside arguments");
            this.kneadingMachine = (KneadingMachine) args[0];
            this.kneadingMachineName = (String) args[1];
            this.doughManagerName = (String) args[2];
        }

        System.out.println(getAID().getLocalName() + " is ready." + "ITS DougManagerName: " + doughManagerName);

        // Register KneadingMachine Agent to the yellow Pages
        // this.register("Kneading-machine", "JADE-bakery");
        this.register(this.kneadingMachineName, "JADE-bakery");

        // Get Agents AIDS
        this.getDoughManagerAIDs();

        // Load bakery information (includes recipes for each product)
        // getbakery();

        // Get KneadingMachines
        // this.getKneadingMachines();

        kneadingCounter.set(0);;
        // Time tracker behavior
        addBehaviour(new timeTracker());
        addBehaviour(new ReceiveProposalRequests());
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

        sd.setType(doughManagerName);
        template.addServices(sd);
        try {


            DFAgentDescription[] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Dough-manager agent:");
            doughManagerAgents = new AID [result.length];
            //doughManagerAgents = new AID ();

             for (int i = 0; i < result.length; ++i) {
                 doughManagerAgents[i] = result[i].getName();
                 System.out.println(doughManagerAgents[i].getName());
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
                if (kneadingInProcess.get()){
                    int curCount = kneadingCounter.incrementAndGet();
                    System.out.println(">>>>> Kneading Counter -> " + kneadingCounter + " <<<<<");
                    addBehaviour(new Kneading());
                }
            }
            if (!messageInProcress.get()){
                baseAgent.finished();
            }
        }
    }

    private class ReceiveProposalRequests extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("kneading-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null){
                String content = msg.getContent();
                System.out.println("I have received a proposal request from " + msg.getSender().getName());

                ACLMessage reply = msg.createReply();
                if (kneadingMachine.isAvailable()){
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Hey I am free, do you wanna use me ;)?");
                }else{
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Sorry, I am married potato :c");
                }
            }else{
                block();
            }
        }
    }

    // Receiving Kneading requests behavior
    private class ReceiveKneadingRequests extends CyclicBehaviour {
        public void action() {

            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                MessageTemplate.MatchConversationId("kneading-request"));

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                messageInProcress.set(true);
                kneadingMachine.setAvailable(false);

                System.out.println(getAID().getLocalName() + " received kneading requests from " + msg.getSender());
                String content = msg.getContent();
                System.out.println("Kneading request contains -> " + content);
                KneadingRequest kneadingRequest = JSONConverter.parseKneadingRequest(content);

                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.CONFIRM);
                reply.setContent("Kneading request was received");
                reply.setConversationId("kneading-request-reply");
                baseAgent.sendMessage(reply);

                kneadingTime = kneadingRequest.getKneadingTime();
                guids = kneadingRequest.getGuids();
                productType = kneadingRequest.getProductType();

                addBehaviour(new Kneading());

            }else {
                block();
            }
        }
    }

    // performs Kneading process
    private class Kneading extends OneShotBehaviour {
        public void action(){
            if (kneadingCounter.get() < kneadingTime){
                if (!kneadingInProcess.get()){
                    System.out.println("----> "+ getAID().getLocalName() + " Kneading for " + kneadingTime);
                    kneadingInProcess.set(true);
                    kneadingMachine.setAvailable(false);
                }

            }else{
                kneadingInProcess.set(false);
                kneadingMachine.setAvailable(true);
                kneadingCounter.set(0);
                System.out.println("Finishing kneading");
                // System.out.println("----> " + guidAvailable + " finished Kneading");
                addBehaviour(new SendKneadingNotification());
            }
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
                System.out.println(getAID().getLocalName() + " My purpose is over ");
                baseAgent.finished();
                // myAgent.doDelete(); //TODO Find when to die
                return true;
            }

           return false;
       }
    }

}
