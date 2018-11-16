package org.mas_maas.agents;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PreparationTableAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private boolean available = true;

    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("PreparationTable", "JADE-bakery");
        this.getDoughManagerAIDs();

        // Create preparationNotification msg
        String preparationNotification = "Preparation-Notification";

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceivePreparationRequests());

        // Creating send kneading notification behaviour
        addBehaviour(new SendPreparationNotification(preparationNotification, doughManagerAgents));
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

    // Receiving Preparation requests behaviour
    private class ReceivePreparationRequests extends CyclicBehaviour {
      public void action() {

          MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

          ACLMessage msg = myAgent.receive(mt);

          if (msg != null) {

              String content = msg.getContent();

              ACLMessage reply = msg.createReply();

              reply.setPerformative(ACLMessage.CONFIRM);

              reply.setContent("Preparation request was received");

              baseAgent.sendMessage(reply);

              Float preparationTime= (float) 2.0;

              addBehaviour(new Preparation(preparationTime));

          }
          else {
              block();
          }
      }
  }

  // performs Preparation process

  private class Preparation extends Behaviour {
      private float preparationTime;
      private float fakeCounter = (float) 0;
      private boolean preparationMsgFlag = true;
      private int step = 0;

      public Preparation(float preparationTime){
          this.preparationTime = preparationTime;
      }

      public void action(){

          switch(step){

                case 0:
                    if (preparationMsgFlag == true){
                        System.out.println(getAID().getLocalName() + "Preparation");
                        preparationMsgFlag = false;
                    }

                    if (fakeCounter == preparationTime){
                        step = 1;

                    }else{
                        fakeCounter++;
                    }
          }

      }
      public boolean done(){
          if (step == 1)
            return true;
          else
            return false;
      }
  }



  // Send a preparationNotification msg to the doughManager agents
  private class SendPreparationNotification extends Behaviour {
    private String preparationNotification;
    private AID [] doughManagerAgents;
    private MessageTemplate mt;
    private int step = 0;

    public SendPreparationNotification(String preparationNotification, AID [] doughManagerAgents){
        this.preparationNotification = preparationNotification;
        this.doughManagerAgents = doughManagerAgents;
    }

       public void action() {

           switch (step) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setContent(preparationNotification);

                    msg.setConversationId("preparation-notification");

                    // Send preparationNotification msg to doughManagerAgents
                    for (int i = 0; i < doughManagerAgents.length; i++){
                        msg.addReceiver(doughManagerAgents[i]);
                    }

                    msg.setReplyWith("msg" + System.currentTimeMillis());

                    baseAgent.sendMessage(msg);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("preparation-notification"),

                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                    step = 1;

                    System.out.println(getAID().getLocalName() + "Sent preparationNotification");
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
