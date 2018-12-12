package org.maas.agents;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class TimeKeeper extends Agent{
	private int currentTimeStep;
	private int countAgentsReplied;

	protected void setup() {
		System.out.println("\tHello! time-teller-agent "+getAID().getLocalName()+" is ready.");

        /* Wait for all the agents to start
         */
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

		addBehaviour(new SendTimeStep());
		addBehaviour(new TimeStepConfirmationBehaviour());
	}

	protected void takeDown() {
        //TODO: call shutdown behaviour
	}

    /* Get the AID for all alive agents
     */
	private List<DFAgentDescription> getAllAgents(){
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			return Arrays.asList(result);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
			return new Vector<DFAgentDescription>();
		}
	}

    /* Send next time step to all agents so that they can proceed with their tasks
     */
	private class SendTimeStep extends OneShotBehaviour {
		public void action() {
            List<DFAgentDescription> agents = getAllAgents();
            currentTimeStep++;
            countAgentsReplied = agents.size();
            System.out.println(">>>>> " + currentTimeStep + " <<<<<");
            for (DFAgentDescription agent : agents) {
				// System.out.println("----> MAAS Agent " + agent.getName());
                ACLMessage timeMessage = new ACLMessage(55);
                timeMessage.addReceiver(agent.getName());
                timeMessage.setContent(Integer.toString(currentTimeStep));
                myAgent.send(timeMessage);
            }
		}
	}

    /* Get `finish` message from all agents (BaseAgent) and once all message are received
     * call SendTimeStep to increment time step
     */
	private class TimeStepConfirmationBehaviour extends CyclicBehaviour {
        private List<AID> agents;

        public TimeStepConfirmationBehaviour(){
            this.agents = new Vector<AID> ();
        }
		public void action() {
			// MessageTemplate rt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            // ACLMessage msg2 = myAgent.receive(rt);
            // if (msg2 != null){
            //     System.out.println("***************************************************************");
            //     System.out.println("----> random message catched by timekeeper" + msg2 .getContent() + " " + msg2.getSender().getName() );
            //     System.out.println("***************************************************************");
            // }
			// System.out.println("sdasdasdasd");
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// System.out.println("**** TimeKeeper received confirmation behavior from " + msg.getSender().getName());
                AID agent = msg.getSender();
                if (!this.agents.contains(agent)){
                    this.agents.add(agent);
                    countAgentsReplied--;
                    if (countAgentsReplied <= 0){
                        myAgent.addBehaviour(new SendTimeStep());
                        this.agents.clear();
                    }
                }
			}
			else {
				block();
			}
		}
	}
}
