package org.mas_maas.agents;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class KneadingMachineAgent extends BaseAgent {
    private AID [] doughManagerAgents;

    private boolean available = true;

    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("KneadingMachine", "JADE-bakery");
        this.getDoughManagerAIDs();

        // Create kneadingNotification msg
        String kneadingNotification = "Kneading-Notification";

        // Creating receive kneading requests behaviour
        addBehaviour(new ReceiveKneadingRequests());

        // Creating send kneading notification behaviour
        addBehaviour(new SendKneadingNotification(kneadingNotification, doughManagerAgents));
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

    // Receiving Kneading requests behaviour
    private class ReceiveKneadingRequests extends CyclicBehaviour {
      public void action() {

          MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);

          ACLMessage msg = myAgent.receive(mt);

          if (msg != null) {

              String content = msg.getContent();

              ACLMessage reply = msg.createReply();

              reply.setPerformative(ACLMessage.CONFIRM);

              reply.setContent("Kneading request was received");

              baseAgent.sendMessage(reply);

              Float keadingTime= (float) 2.0;

              addBehaviour(new Kneading(keadingTime));

          }
          else {
              block();
          }
      }
  }

  // performs Kneading process

  private class Kneading extends Behaviour {
      private float kneadingTime;
      private float fakeCounter = (float) 0;
      private boolean kneadingMsgFlag = true;
      private int step = 0;

      public Kneading(float kneadingTime){
          this.kneadingTime = kneadingTime;
      }

      public void action(){

          switch(step){

                case 0:
                    if (kneadingMsgFlag == true){
                        System.out.println(getAID().getLocalName() + "Kneading");
                        kneadingMsgFlag = false;
                    }

                    if (fakeCounter == kneadingTime){
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



  // Send a kneadingNotification msg to the doughManager agents
  private class SendKneadingNotification extends Behaviour {
    private String kneadingNotification;
    private AID [] doughManagerAgents;
    private MessageTemplate mt;
    private int step = 0;

    public SendKneadingNotification(String kneadingNotification, AID [] doughManagerAgents){
        this.kneadingNotification = kneadingNotification;
        this.doughManagerAgents = doughManagerAgents;
    }

       public void action() {

           switch (step) {
                case 0:
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

                    msg.setContent(kneadingNotification);

                    msg.setConversationId("kneading-notification");

                    // Send kneadingNotification msg to doughManagerAgents
                    for (int i = 0; i < doughManagerAgents.length; i++){
                        msg.addReceiver(doughManagerAgents[i]);
                    }

                    msg.setReplyWith("msg" + System.currentTimeMillis());

                    baseAgent.sendMessage(msg);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("kneading-notification"),

                    MessageTemplate.MatchInReplyTo(msg.getReplyWith()));

                    step = 1;

                    System.out.println(getAID().getLocalName() + "Sent kneadingNotification");
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
