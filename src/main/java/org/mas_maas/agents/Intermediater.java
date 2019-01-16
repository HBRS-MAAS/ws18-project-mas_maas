package org.mas_maas.agents;

import org.maas.messages.CoolingRequest;

import com.google.gson.Gson;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import org.maas.agents.BaseAgent;

// This is a dummy agent for testing communication with the CoolingRackAgent.
// TODO: Time tracking should be integrated to this agent

public class Intermediater extends BaseAgent {

    private AID [] coolingRackAgents;
    private int coolingRequestCounter = 0;

    protected void setup() {
        super.setup();
        System.out.println("Hello, intermediater" + getAID().getLocalName() + " is ready.");

        // Register the Baking-manager in the yellow pages
        this.register("intermediater", "JADE-bakery");

        this.getCoolingRackAIDs();
        // Send dummy coolingRequest
        CoolingRequest coolingRequest = createCoolingRequest();
        Gson gson = new Gson();
        String coolingRequestString = gson.toJson(coolingRequest);

        // hacky way to remove the charaters before the json array section of the string and the '}' after
        coolingRequestString = coolingRequestString.replaceFirst(".*?:", "");
        coolingRequestString = coolingRequestString.substring(0, coolingRequestString.length() - 1);

        //String coolingRequestString = JsonConverter.getJsonString(coolingRequest);

        System.out.println("Cooling request: " + coolingRequestString);

        addBehaviour(new RequestCooling(coolingRequestString, coolingRequestCounter));
        coolingRequestCounter ++;

    }

    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
        this.deRegister();
    }

    public void getCoolingRackAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("cooling-rack-agent");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Cooling Rack agents:");
            coolingRackAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                coolingRackAgents[i] = result[i].getName();
                System.out.println(coolingRackAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    public CoolingRequest createCoolingRequest() {

        // Creates a dummy cooling Request Message for testing

        String guid = "Donut";
        int quantity = 7;
        float coolingDuration = 1;

        CoolingRequest coolingRequest = new CoolingRequest();
        coolingRequest.addCoolingRequest(guid, coolingDuration, quantity);

        return coolingRequest;

    }

    //This is the behaviour used for sending a CoolingRequest
    private class RequestCooling extends Behaviour{
        private String coolingRequest;
        private int coolingRequestcounter;
        private MessageTemplate mt;
        private int option = 0;

        public RequestCooling(String coolingRequest, int coolingRequestCounter){
            this.coolingRequest = coolingRequest;
            this.coolingRequestcounter = coolingRequestCounter;
        }
        public void action(){
            //blocking action
            // if (!baseAgent.getAllowAction()) {
            //     return;
            // }
            switch(option){
                case 0:

                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.setContent(coolingRequest);
                    msg.setConversationId("cooling-request");

                    // Send kneadingRequest msg to all kneadingMachineAgents
                    for (int i=0; i<coolingRackAgents.length; i++){
                        msg.addReceiver(coolingRackAgents[i]);
                    }
                    // msg.setReplyWith("msg"+System.currentTimeMillis());
                    baseAgent.sendMessage(msg);  // calling sendMessage instead of send

                    option = 1;
                    System.out.println(getLocalName()+" Sent coolingRequest" + coolingRequest);
                    break;

                case 1:
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                        MessageTemplate.MatchConversationId("cooling-request-reply"));

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
        public boolean done(){
            if (option == 2){
                // baseAgent.finished();
                return true;

            }
            return false;
        }
    }



}
