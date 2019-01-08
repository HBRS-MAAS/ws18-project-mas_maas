package org.mas_maas.agents;
import java.util.Vector;

import org.maas.JSONConverter;
import org.maas.Objects.Bakery;
import org.maas.Objects.OrderMas;
import org.maas.Objects.Client;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

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

public class DummyOrderProcesser extends BaseAgent {
    private AID [] doughManagerAgents;
    private Vector<Bakery> bakeries;
    private String scenarioPath;
    private Vector<String> doughManagerAgentNames = new Vector<String>();
    private Vector<OrderMas> orders = new Vector<OrderMas>();

    protected void setup(){
        super.setup();
        Object[] args = getArguments();
		if (args != null && args.length > 0) {
			this.scenarioPath = (String) args[0];
		}

        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("dummyOrderProcesser", "JADE-bakery");

        getBakery(this.scenarioPath);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getDoughManagerNames();
        getDoughManagerAIDs();
        try {
			getOrderInfo();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        //processOrders();

	}

    public void getBakery(String scenarioPath){
        // Select the scenario file to use
        // guid is the name of the bakery
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

    public void getDoughManagerNames(){
        Bakery bakery = bakeries.get(0);
        String doughManagerAgentName = "DoughManagerAgent_" + bakery.getGuid();
        System.out.println("---> I wanna find "+ doughManagerAgentName);
        doughManagerAgentNames.add(doughManagerAgentName);
    }

    public void getDoughManagerAIDs() {
        /*
        Object the AID of all the dough-manager agents found
        */
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        doughManagerAgents = new AID [doughManagerAgentNames.size()];

        int j = 0;

        for(String doughManagerAgentName : doughManagerAgentNames) {

            sd.setType(doughManagerAgentName);
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(this, template);
                System.out.println("-----> " + getAID().getLocalName() + "Found the following Dough-manager agent:");
                // doughManagerAgents = new AID [result.length];
                //doughManagerAgents = new AID ();
                for (int i = 0; i < result.length; ++i) {
                    doughManagerAgents[j] = result[i].getName();
                    System.out.println(doughManagerAgents[j].getName());
                }
            }
            catch (FIPAException fe) {
                System.out.println("-----> Failed to find " + doughManagerAgentName);
                fe.printStackTrace();
            }
            j++;
        }
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
        OrderMas order = orders.get(0);
        // for (Order order : orders){
            String doughManagerName = doughManagerAgents[0].getName();
            System.out.println("---> My object " + order);
            String orderString = gson.toJson(order);
            System.out.println("---> My converted object " + orderString);
            System.out.println("---> My converted object " + order.toString());
            addBehaviour(new sendOrder(orderString, doughManagerName));
            // }
    }

// Send a kneadingNotification msg to the doughManager agents
    private class sendOrder extends Behaviour {
        private MessageTemplate mt;
        private int option = 0;
        private Gson gson = new Gson();
        private String orderString;
        private String doughManagerName;

        private sendOrder(String orderString, String doughManagerName){
            this.orderString = orderString;
            this.doughManagerName = doughManagerName;
        }

    public void action() {
        switch (option) {
            case 0:

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(orderString);
                // msg.setConversationId("sending-Order"+doughManagerName);

                // Send kneadingNotification msg to doughManagerAgents
                for (int i = 0; i < doughManagerAgents.length; i++){
                    if (doughManagerAgents[i].getName().equals(doughManagerName)){
                        msg.addReceiver(doughManagerAgents[i]);
                    }
                }
                baseAgent.sendMessage(msg);

                option = 1;
                System.out.println("----> " + getAID().getLocalName() + " Sent Order to dough manager " + doughManagerName );
                break;

            case 1:
                mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                    MessageTemplate.MatchConversationId("reply-Order"));

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
