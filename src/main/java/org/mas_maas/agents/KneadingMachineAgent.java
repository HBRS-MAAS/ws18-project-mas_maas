package org.mas_maas.agents;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.maas.JSONConverter;
import org.maas.messages.KneadingNotification;
import org.maas.messages.KneadingRequest;
// import org.mas_maas.objects.Bakery;
import org.maas.Objects.Equipment;
import org.maas.Objects.KneadingMachine;

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
    private AID doughManagerAgent;

    private AtomicBoolean kneadingInProcess = new AtomicBoolean(false);
    // private AtomicBoolean messageInProcress = new AtomicBoolean(false);
    private AtomicInteger messageProcessing = new AtomicInteger(0);
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
            this.kneadingMachine = (KneadingMachine) args[0];
            this.kneadingMachineName = (String) args[1];
            this.doughManagerName = (String) args[2];
        }

        this.getDoughManagerAID();

        System.out.println("Hello! " + getAID().getLocalName() + " is ready." + "its DougManager is: " + doughManagerName);

        this.register(this.kneadingMachineName, "JADE-bakery");

        kneadingMachine.setAvailable(true);


        // Load bakery information (includes recipes for each product)
        // getbakery();

        // Get KneadingMachines
        // this.getKneadingMachines();

        kneadingCounter.set(0);
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

    public void getDoughManagerAID() {
        doughManagerAgent = new AID (doughManagerName, AID.ISLOCALNAME);

    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }else{
                if (kneadingInProcess.get()){
                    int curCount = kneadingCounter.incrementAndGet();
                    System.out.println(">>>>> Kneading Counter -> " + getAID().getLocalName() + " " + kneadingCounter + " <<<<<");
                    addBehaviour(new Kneading());
                }
            }
            // if (!messageInProcress.get()){
            //     baseAgent.finished();
            // }
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
                MessageTemplate.MatchConversationId("kneading-request"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null){
                // messageInProcress.set(true);
                String content = msg.getContent();
                //System.out.println(getAID().getLocalName() + "has received a proposal request from " + msg.getSender().getName());

                ACLMessage reply = msg.createReply();
                if (kneadingMachine.isAvailable()){
                	//System.out.println(getAID().getLocalName() + " is available");
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent("Hey I am free, do you wanna use me ;)?");
                }else{
                	System.out.println(getAID().getLocalName() + " is unavailable");
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Sorry, I am married potato :c");
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

    // Receiving Kneading requests behavior
    private class ReceiveKneadingRequests extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                MessageTemplate.MatchConversationId("kneading-request"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // messageInProcress.set(true);
                if (kneadingMachine.isAvailable()){
                    kneadingMachine.setAvailable(false);

                    String content = msg.getContent();
                    System.out.println(getAID().getLocalName() + " WILL perform Kneading for " + msg.getSender() + "Kneading information -> " + content);

                    KneadingRequest kneadingRequest = JSONConverter.parseKneadingRequest(content);

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent("Kneading request was received");
                    reply.setConversationId("kneading-request");
                    baseAgent.sendMessage(reply);

                    kneadingTime = kneadingRequest.getKneadingTime();
                    guids = kneadingRequest.getGuids();
                    productType = kneadingRequest.getProductType();

                    // messageInProcress.set(false);
                    addBehaviour(new Kneading());
                }
                else{
                    // System.out.println(getAID().getLocalName()  + " is already taken");

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("KneadingMachine is taken");
                    reply.setConversationId("kneading-request");
                    baseAgent.sendMessage(reply);
                    System.out.println(getAID().getLocalName() + " failed kneading of " + msg.getContent());

                }
                messageProcessing.decrementAndGet();


            }else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    // performs Kneading process
    private class Kneading extends OneShotBehaviour {
        public void action(){
            if (kneadingCounter.get() < kneadingTime){
                if (!kneadingInProcess.get()){
                    System.out.println(getAID().getLocalName() + " Kneading for " + kneadingTime);
                    kneadingInProcess.set(true);
                    kneadingMachine.setAvailable(false);
                }

            }else{
                kneadingInProcess.set(false);
                kneadingMachine.setAvailable(true);
                kneadingCounter.set(0);
                System.out.println(getAID().getLocalName() + "Finishing kneading");
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
            messageProcessing.incrementAndGet();
            // messageInProcress.set(true);
            switch (option) {
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    // System.out.println("-----> Kneading notification string " + kneadingNotificationString);
                    msg.setContent(kneadingNotificationString);
                    msg.setConversationId("kneading-notification");

                    msg.addReceiver(doughManagerAgent);

                    baseAgent.sendMessage(msg);

                    System.out.println(getAID().getLocalName() + " Sent kneadingNotification");
                    messageProcessing.decrementAndGet();
                    option = 1;
                    break;

                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("kneading-notification-reply"));

                    ACLMessage reply = baseAgent.receive(mt);

                    if (reply != null) {
                        System.out.println(getAID().getLocalName() + " Received kneading notification confirmation from " + reply.getSender());
                        option = 2;
                        // messageInProcress.set(false);
                    }
                    else {
                        // messageInProcress.set(false);
                        block();
                    }
                    messageProcessing.decrementAndGet();
                    break;

                default:
                    // messageInProcress.set(false);
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
