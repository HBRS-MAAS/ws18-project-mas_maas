package org.mas_maas.agents;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class TimeKeeper extends Agent{
    private int currentTimeStep;
    private int countAgentsReplied;

    protected void setup() {
        System.out.println("Hallo! time-teller-agent "+getAID().getLocalName()+" is ready.");

        /* Wait for all the agents to start
         */
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        addBehaviour(new SendTimeStep());
        addBehaviour(new TimeStepConfirmationBehaviour());
        addBehaviour(new shutdown());
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

            for (DFAgentDescription agent : agents) {
                ACLMessage timeMessage = new ACLMessage(ACLMessage.INFORM);
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
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                countAgentsReplied--;
                if (countAgentsReplied <= 0){
                    // before incrementing time step, make sure there are agents we need to talk to, otherwise shutdown
                    List<DFAgentDescription> agents = getAllAgents();
                    if (agents.isEmpty())
                    {
                        myAgent.addBehaviour(new shutdown());
                    } else {
                        myAgent.addBehaviour(new SendTimeStep());
                    }
                }
            }
            else {
                block();
            }
        }
    }

    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
    private class shutdown extends OneShotBehaviour {
        public void action() {
            ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
            Codec codec = new SLCodec();
            myAgent.getContentManager().registerLanguage(codec);
            myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
            shutdownMessage.addReceiver(myAgent.getAMS());
            shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
            shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
            try {
                myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
                myAgent.send(shutdownMessage);
            }
            catch (Exception e) {
                //LOGGER.error(e);
            }

        }
    }
}
