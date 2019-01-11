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

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class DummyOrderProcesser extends BaseAgent {
    private ArrayList<AID> doughManagerAgents = new ArrayList<AID>();
    private Vector<Bakery> bakeries;
    private String scenarioPath;
    private Vector<OrderMas> orders = new Vector<OrderMas>();
    private AtomicBoolean messageInProcress = new AtomicBoolean(false);


    protected void setup(){
        super.setup();
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.scenarioPath = (String) args[0];
        }

        System.out.println("Hello! " + getAID().getLocalName() + " is ready.");
        this.register("DummyOrderProcesser", "JADE-bakery");

        getBakeries(this.scenarioPath);

        getDoughManagerAIDs();
        try {
            //Read the orders from the scenarioPath
            getOrderInfo();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // try {
        //     Thread.sleep(3000);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        addBehaviour(new timeTracker());
        processOrders();

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
            if (!messageInProcress.get())
            {
                baseAgent.finished();
            }
        }
    }

    public void getBakeries(String scenarioPath){
        String jsonDir = scenarioPath;
        try {
            // System.out.println("Working Directory = " + System.getProperty("user.dir"));
            String bakeryFile = new Scanner(new File(jsonDir + "bakeries.json")).useDelimiter("\\Z").next();
            this.bakeries = JSONConverter.parseBakeries(bakeryFile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void getDoughManagerAIDs(){
        // For now get just the first one to test
        //for (Bakery bakery : bakeries) {
        Bakery bakery = bakeries.get(0);
            String doughManagerAgentName = "DoughManager_" + bakery.getGuid();
            doughManagerAgents.add(new AID (doughManagerAgentName, AID.ISLOCALNAME));
        //}
    }

    private void getOrderInfo() throws FileNotFoundException{
        String clientFile = new Scanner(new File(this.scenarioPath+ "clients.json")).useDelimiter("\\Z").next();
        Vector<Client> clients = JSONConverter.parseClients(clientFile);
        for (Client client : clients){
            for (OrderMas order : client.getOrders()){
                // System.out.println(order);
                orders.add(order);
            }
        }
    }

    public void processOrders(){
        Gson gson = new Gson();
        Random rand = new Random();
        OrderMas order = orders.get(0);
        // for (Order order : orders){
            //Randomly select a DoughManager to send the order to
            int doughManagerIndex = rand.nextInt(doughManagerAgents.size());
            AID doughManagerAgent = doughManagerAgents.get(doughManagerIndex);
            System.out.println("Order will be sent to: " + doughManagerAgent);
            System.out.println("Order object " + order);
            String orderString = gson.toJson(order);

            addBehaviour(new sendOrder(orderString, doughManagerAgent));
            // }
    }

// Send a kneadingNotification msg to the doughManager agents
    private class sendOrder extends Behaviour {
        private MessageTemplate mt;
        private int option = 0;
        private Gson gson = new Gson();
        private String orderString;
        private AID doughManagerAgent;

        private sendOrder(String orderString, AID doughManagerAgent){
            this.orderString = orderString;
            this.doughManagerAgent = doughManagerAgent;
        }

    public void action() {

        // Don't allow any action while processing this message
        // TODO: Change to CFP ?
        messageInProcress.set(true);
        switch (option) {
            case 0:

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(orderString);
                // msg.setConversationId("sending-Order"+doughManagerName);

                // Send kneadingNotification msg to doughManagerAgents
                // for (int i = 0; i < doughManagerAgents.size(); i++){
                //     if (doughManagerAgents.get(i).getName().equals(doughManagerName)){
                //         msg.addReceiver(doughManagerAgents.get(i));
                //     }
                // }
                msg.addReceiver(doughManagerAgent);
                baseAgent.sendMessage(msg);

                option = 1;
                System.out.println(getAID().getLocalName() + " Sent Order to dough manager " + doughManagerAgent );
                messageInProcress.set(false);
                break;

            case 1:
                mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                    MessageTemplate.MatchConversationId("reply-Order"));

                ACLMessage reply = baseAgent.receive(mt);

                if (reply != null) {
                    System.out.println(getAID().getLocalName() + " Received confirmation from " + reply.getSender());
                    option = 2;
                    messageInProcress.set(false);
                }
                else {
                    messageInProcress.set(false);
                    block();
                }
                break;

            default:
                messageInProcress.set(false);
                break;
        }
    }

    public boolean done() {
        if (option == 2) {
            //System.out.println(getAID().getLocalName() + " My purpose is over ");
            //baseAgent.finished();
            // myAgent.doDelete(); //TODO Find when to die
            return true;
        }

       return false;
    }
    }
}
