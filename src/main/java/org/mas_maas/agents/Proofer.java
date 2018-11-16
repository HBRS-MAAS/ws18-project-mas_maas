package org.mas_maas.agents;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Proofer extends BaseAgent {
    private AID [] doughManagerAgents;
    private AID [] bakingInterfaceAgents;
    private boolean available = true;

    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("Proofer", "JADE-bakery");
        this.getDoughManagerAIDs();
        this.getBakingInterfaceAIDs();
        addBehaviour(new ReceiveProofingRequests());

        // Create doughNotification msg
        String doughNotification = "Dough-Notification";

        addBehaviour(new SendDoughNotification(doughNotification, bakingInterfaceAgents));

    }

    public void getDoughManagerAIDs() {
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

    public void getBakingInterfaceAIDs() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Baking-interface");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println(getAID().getLocalName() + "Found the following Baking-interface agents:");
            bakingInterfaceAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                bakingInterfaceAgents[i] = result[i].getName();
                System.out.println(bakingInterfaceAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

      /* This is the behaviour used for receiving proofing requests */
    private class ReceiveProofingRequests extends CyclicBehaviour {
      public void action() {
          // baseAgent.finished(); //call it if there are no generic behaviours
          MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
          ACLMessage msg = myAgent.receive(mt);
          if (msg != null) {
              String content = msg.getContent();
              ACLMessage reply = msg.createReply();
              reply.setPerformative(ACLMessage.CONFIRM);
              reply.setContent("Proofing request was received");
              baseAgent.sendMessage(reply);
              // TODO convert String to proofingRequest object
              // Start a timer that waits for proofing time
              // Set the agent to unavailable
              // After the timer is done, set it to available
              Float proofingTime= (float) 2.0;
              addBehaviour(new Proofing(proofingTime));

          }
          else {
              block();
          }
      }
  }

  // This is the behaviour that performs the proofing process.

  private class Proofing extends Behaviour {
      private float proofingTime;
      private int step = 0;

      public Proofing(float proofingTime){
          this.proofingTime = proofingTime;
      }
      public void action(){
          switch(step){
              case 0:
              System.out.println(getAID().getLocalName() + "Proofing");
              // wait unitl it receives a time step of the TimeKeeper agent
              step = 1;

          }

      }
      public boolean done(){
          if (step == 1)
            return true;
          else
            return false;
      }
  }



  // This is the behaviour used for sending a doughNotification msg to the BakingInterface agent
  private class SendDoughNotification extends Behaviour {
    private String doughNotification;
    private AID [] bakingInterfaceAgents;
    private MessageTemplate mt;
    private int step = 0;

    public SendDoughNotification(String doughNotification, AID [] bakingInterfaceAgents){
        this.doughNotification = doughNotification;
        this.bakingInterfaceAgents = bakingInterfaceAgents;
    }

       public void action() {
           // blocking action
           // if (!baseAgent.getAllowAction()) {
           //     return;
           // }
           switch (step) {
           case 0:
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent(doughNotification);
                msg.setConversationId("baking-request");

                // Send doughNotification msg to bakingInterfaceAgents
                for (int i=0; i<bakingInterfaceAgents.length; i++){
                  msg.addReceiver(bakingInterfaceAgents[i]);
                }
                msg.setReplyWith("msg"+System.currentTimeMillis());
                baseAgent.sendMessage(msg);  // calling sendMessage instead of send
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("trade-example"),
                MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                step = 1;
                System.out.println(getAID().getLocalName() + "Sending doughNotification");
                break;
           case 1:
               ACLMessage reply = baseAgent.receive(mt);
               if (reply != null) {
                   if (reply.getPerformative() == ACLMessage.CONFIRM) {
                       System.out.println(getAID().getLocalName() + "Received confirmation");
                       step = 2;
                   }
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
           if (step == 2) {
               baseAgent.finished(); // calling finished method
               myAgent.doDelete();
               return true;
           }
           return false;
       }
   }

}
