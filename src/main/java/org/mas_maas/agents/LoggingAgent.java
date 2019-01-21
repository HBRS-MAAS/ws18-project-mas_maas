package org.mas_maas.agents;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.maas.JSONConverter;
import org.maas.Objects.Bakery;
import org.maas.Objects.Client;
import org.maas.Objects.OrderMas;
import org.maas.agents.BaseAgent;
import org.maas.Objects.DoughPrepTable;
import org.maas.Objects.Equipment;
import org.maas.Objects.KneadingMachine;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class LoggingAgent extends BaseAgent {
    private ArrayList<AID> doughManagerAgents = new ArrayList<AID>();
    private ArrayList<AID> kneadingMachineAgents = new ArrayList<AID>();
    private ArrayList<AID> preparationTableAgents = new ArrayList<AID>();
    private ArrayList<AID> prooferAgents = new ArrayList<AID>();
    private ArrayList<AID> bakingInterfaceAgents = new ArrayList<AID>();
    private ArrayList<AID> ovenAgents = new ArrayList<AID>();
    private ArrayList<AID> bakingPreparationAgents = new ArrayList<AID>();
    private ArrayList<AID> coolingRackAgents = new ArrayList<AID>();
    private Vector<Bakery> bakeries;
    private String scenarioPath;
    private AtomicInteger messageProcessing = new AtomicInteger(0);
    // private Vector<Equipment> equipments;

    protected void setup(){
        super.setup();
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.scenarioPath = (String) args[0];
        }

        this.register("LoggingAgent", "JADE-bakery");
        System.out.println("Hello! " + getAID().getLocalName() + " is ready.");

        // getBakeries(this.scenarioPath);
        // getDoughManagerAIDs();
        // getEquipmentAgentsAIDs();
        // getProoferAIDs();
        // getBakingInterfaceAIDs();
        // getOvenAIDs();
        // getBakingPreparationAIDs();
        // getCoolingRackAIDs();

        addBehaviour(new timeTracker());
        addBehaviour(new ReceiveKneadingNotification());
        addBehaviour(new ReceivePreparationNotification());
        addBehaviour(new ReceiveDoughNotification());
        addBehaviour(new ReceiveBakingNotification());
        addBehaviour(new ReceiveBakingPreparationNotification());
        addBehaviour(new ReceiveCooling());

    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
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

//     public void getBakeries(String scenarioPath){
//         String jsonDir = scenarioPath;
//         try {
//             // System.out.println("Working Directory = " + System.getProperty("user.dir"));
//             String bakeryFile = new Scanner(new File(jsonDir + "bakeries.json")).useDelimiter("\\Z").next();
//             this.bakeries = JSONConverter.parseBakeries(bakeryFile);
//         } catch (FileNotFoundException e) {
//             // TODO Auto-generated catch block
//             e.printStackTrace();
//         }
//     }
//
//     public void getDoughManagerAIDs(){
//         // For now get just the first one to test
//         for (Bakery bakery : bakeries) {
//         //Bakery bakery = bakeries.get(0);
//             String doughManagerAgentName = "DoughManager_" + bakery.getGuid();
//             doughManagerAgents.add(new AID (doughManagerAgentName, AID.ISLOCALNAME));
//         }
//     }
//
//     public void getBakingInterfaceAIDs(){
//         // For now get just the first one to test
//         for (Bakery bakery : bakeries) {
//         //Bakery bakery = bakeries.get(0);
//             String bakingInterfaceAgentName = "BakingInterface_" + bakery.getGuid();
//             bakingInterfaceAgents.add(new AID (bakingInterfaceAgentName, AID.ISLOCALNAME));
//         }
//     }
//
//     public void getOvenAIDs(){
//         // For now get just the first one to test
//         for (Bakery bakery : bakeries) {
//         //Bakery bakery = bakeries.get(0);
//             String ovenAgentName = "OvenAgent_" + bakery.getGuid();
//             ovenAgents.add(new AID (ovenAgentName, AID.ISLOCALNAME));
//         }
//     }
//
//     public void getProoferAIDs(){
//         // For now get just the first one to test
//         for (Bakery bakery : bakeries) {
//         //Bakery bakery = bakeries.get(0);
//             String prooferAgentName = "Proofer_" + bakery.getGuid();
//             prooferAgents.add(new AID (prooferAgentName, AID.ISLOCALNAME));
//         }
//     }
//
//     public void getCoolingRackAIDs(){
//         // For now get just the first one to test
//         for (Bakery bakery : bakeries) {
//         //Bakery bakery = bakeries.get(0);
//             AID coolingRacksAgent = new AID(bakery.getGuid() + "-cooling-rack", AID.ISLOCALNAME);
//             coolingRackAgents.add(coolingRacksAgent);
//         }
//     }
//
//     public void getBakingPreparationAIDs(){
//         for (Bakery bakery : bakeries) {
//         //Bakery bakery = bakeries.get(0);
//             String bakingPreparationAgentName = "BakingPreparationAgent_" +  bakery.getGuid();
//             bakingPreparationAgents.add(new AID (bakingPreparationAgentName, AID.ISLOCALNAME));
//         }
//     }
//
//     public void getEquipmentAgentsAIDs(){
//         for (Bakery bakery : bakeries){
//             Vector<Equipment> equipments;
//             equipments = bakery.getEquipment();
//
//             for (Equipment equipment : equipments) {
//
//                 if (equipment instanceof KneadingMachine){
//
//                     // Object of type KneadingMachine
//                     KneadingMachine kneadingMachine = (KneadingMachine) equipment;
//                     // Name of the kneadingMachineAgent
//                     String kneadingMachineAgentName = "KneadingMachineAgent_" +  bakery.getGuid() + "_" + kneadingMachine.getGuid();
//
//                     // kneadingMachineNames.add(kneadingMachineAgentName);
//                     kneadingMachineAgents.add(new AID (kneadingMachineAgentName, AID.ISLOCALNAME));
//                 }
//
//                 if (equipment instanceof DoughPrepTable){
//
//                     //Object of type DoughPrepTable
//                     DoughPrepTable doughPrepTable = (DoughPrepTable) equipment;
//
//                     //Name of preparationTableAgent
//                     String doughPrepTableAgentName = "DoughPrepTableAgent_" +  bakery.getGuid() + "_" + doughPrepTable.getGuid();
//
// //                    doughPrepTableNames.add(doughPrepTableAgentName);
//                     preparationTableAgents.add(new AID(doughPrepTableAgentName, AID.ISLOCALNAME));
//
//                 }
//             }
//
//         }
//     }

    private class ReceiveKneadingNotification extends CyclicBehaviour {
        public void action() {

            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("kneading-notification"));
            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("================================================================================");
                System.out.println(" _____>>> " + getAID().getLocalName()+" Received Kneading Notification from " + msg.getSender()
                + " for: " + msg.getContent());
                // System.out.println("================================================================================");
                // String kneadingNotificationString = msg.getContent();
                //
                // // Convert kneadingNotificationString to kneadingNotification object
                // KneadingNotification kneadingNotification = JSONConverter.parseKneadingNotification(kneadingNotificationString);
                // String productType = kneadingNotification.getProductType();
                // Vector<String> guids = kneadingNotification.getGuids();

                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    private class ReceivePreparationNotification extends CyclicBehaviour {
        public void action() {

            // insure we don't allow a time step until we are done processing this message
            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("preparation-notification"));
            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("======================================");
                System.out.println( " _____>>> " + getAID().getLocalName()+" Received Preparation Notification from " + msg.getSender() + " " + msg.getContent());
                // System.out.println("======================================");
                // String preparationNotificationString = msg.getContent();
                //
                // // Convert preparationNotificationString to preparationNotification object
                // PreparationNotification preparationNotification = JSONConverter.parsePreparationNotification(preparationNotificationString);
                // String productType = preparationNotification.getProductType();
                // Vector<String> guids = preparationNotification.getGuids();
                //
                // // Add guids with this productType to the queueProofing
                // queueProofing(productType, guids);
                messageProcessing.decrementAndGet();
            }
            else {
                block();
                messageProcessing.decrementAndGet();
            }
        }

    }

    private class ReceiveDoughNotification extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("dough-Notification"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("================================================================================");
                System.out.println( " _____>>> " + getAID().getLocalName()+" Received dough Notification from \n \t" + msg.getSender()
                    + " for: " + msg.getContent());
                // System.out.println("================================================================================");
                // String doughNotificationString = msg.getContent();
                // // System.out.println("Dough notification contains -> " +doughNotificationString);
                // DoughNotification doughNotification = JSONConverter.parseDoughNotification(doughNotificationString);
                // String productType = doughNotification.getProductType();
                // Vector<String> guids = doughNotification.getGuids();
                // Vector<Integer> productQuantities = doughNotification.getProductQuantities();
                // //
                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    private class ReceiveBakingNotification extends CyclicBehaviour {
        public void action() {
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("baking-notification"));
            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("======================================");
                System.out.println(" _____>>> " +getAID().getLocalName()+" Received Baking Notification from "
               + msg.getSender() + " for: " + msg.getContent());
                // System.out.println("======================================");
                // String bakingNotificationString = msg.getContent();
                // // Convert bakingNotificationString to bakingNotification object
                // BakingNotification bakingNotification = JSONConverter.parseBakingNotification(bakingNotificationString);
                // String productType = bakingNotification.getProductType();
                // Vector<String> guids = bakingNotification.getGuids();
                // Vector<Integer> productQuantities = bakingNotification.getProductQuantities();

                // Add guids with this productType to the queuePreparation
                // queuePreparation(productType, guids, productQuantities);
                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    private class ReceiveBakingPreparationNotification extends CyclicBehaviour {
        public void action() {

            messageProcessing.incrementAndGet();
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("preparationBaking-notification"));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {
                // System.out.println("======================================");
                System.out.println(" _____>>> " + getAID().getLocalName()+" Received Baking Preparation Notification from "
               + msg.getSender() + " for: " + msg.getContent());
                // System.out.println("======================================");
                // String preparationNotificationString = msg.getContent();
                //
                // // Convert preparationNotificationString to preparationNotification object
                // PreparationNotification preparationNotification = JSONConverter.parsePreparationNotification(preparationNotificationString);
                //
                // String productType = preparationNotification.getProductType();
                // Vector<String> guids = preparationNotification.getGuids();
                // Vector<Integer> productQuantities = preparationNotification.getProductQuantities();


                messageProcessing.decrementAndGet();
            }
            else {
                messageProcessing.decrementAndGet();
                block();
            }
        }
    }

    private class ReceiveCooling extends CyclicBehaviour{
        private int coolingRequestCounter = 0;

        public void action(){
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {

            	String content = msg.getContent();
                // System.out.println("================================================================================");
                System.out.println(" _____>>> " + getAID().getLocalName()+" received cooling notifications from \n \t" + msg.getSender()
                    + " for: \n \t" + content);
                coolingRequestCounter++;
                // System.out.println("================================================================================");

                // System.out.println(getAID().getLocalName()+" sent coolingRequest of " + content + " to " + coolingRacksAgent.getName());
                // bakingTemp = bakingRequest.getBakingTemp();
                // Float bakingTime = bakingRequest.getBakingTime();
                // productType = bakingRequest.getProductType();
                // guids = bakingRequest.getGuids();
                // productQuantities = bakingRequest.getProductQuantities();

                // addBehaviour(new Baking(bakingTime));
                messageProcessing.decrementAndGet();

            }

            else {
                messageProcessing.decrementAndGet();
                block();
            }

        }
    }
}
