package org.mas_maas.agents;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.maas.JSONConverter;
import org.maas.messages.PreparationNotification;
import org.maas.messages.PreparationRequest;
import org.maas.utils.Time;
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

public class PostBakingProcessor extends BaseAgent {

    private AtomicInteger messageProcessing = new AtomicInteger(0);

    private AID bakingManagerAgent;
    private AID coolingRacksAgent;

    private String postBakingProcessorAgentName;
    private String bakingManagerName;
    private String bakeryId;

    protected void setup() {
        super.setup();

        Object[] args = getArguments();

        if(args != null && args.length > 0){
            this.postBakingProcessorAgentName = (String) args[0];
            this.bakingManagerName = (String) args[1];
            this.bakeryId = (String) args[2];
        }

        this.getBakingManagerAID();
        this.getcoolingRackAID();

        System.out.println("Hello! " + getAID().getLocalName() + " is ready." + "its bakingManager is: " + bakingManagerAgent.getName());

        this.register(this.postBakingProcessorAgentName, "JADE-bakery");

        addBehaviour(new timeTracker());
        addBehaviour(new ReceiveAndRequestCooling());
    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getBakingManagerAID() {
        bakingManagerAgent = new AID (bakingManagerName, AID.ISLOCALNAME);
    }

    public void getcoolingRackAID() {
        coolingRacksAgent = new AID(bakeryId + "-cooling-rack", AID.ISLOCALNAME);
    }

    private class timeTracker extends CyclicBehaviour {
        public void action() {
            if (!baseAgent.getAllowAction()) {
                return;
            }

            if (messageProcessing.get() <= 0){

                baseAgent.finished();
            }
        }
    }

    private class ReceiveAndRequestCooling extends CyclicBehaviour{

        public void action(){
            messageProcessing.incrementAndGet();

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchSender(bakingManagerAgent));

            ACLMessage msg = baseAgent.receive(mt);

            if (msg != null) {

            	String content = msg.getContent();

                System.out.println("================================================================================");
                System.out.println(getAID().getLocalName()+" received cooling requests from \n \t" + msg.getSender()
                    + " for: \n \t" + content);
                System.out.println("================================================================================");

                ACLMessage loadingBayMessage = new ACLMessage(ACLMessage.INFORM);

                loadingBayMessage.addReceiver(coolingRacksAgent);
                loadingBayMessage.setConversationId(msg.getConversationId());
                loadingBayMessage.setContent(content);

                baseAgent.sendMessage(loadingBayMessage);

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
