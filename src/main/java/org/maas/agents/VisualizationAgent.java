/*import org.right_brothers.visualizer.ui.Visualizer;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;


@SuppressWarnings("serial")
public class VisualizationAgent extends Agent {

    private Visualizer guiWindow;
    public int counter;

    protected void setup() {
    // Printout a welcome message
        System.out.println("Hello! Visualization-agent "+getAID().getName()+" is ready.");

        this.counter = 0;
        // launch the gui window in another thread
        new Thread() {
            @Override
            public void run() {
                Visualizer.run(new String[] {});
            }
        }.start();
        guiWindow = Visualizer.waitForInstance();
        addBehaviour(new MessageServer());
    }
    protected void takeDown() {
        System.out.println(getAID().getLocalName() + ": Terminating.");
    }

    private class MessageServer extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                String msgString =  msg.getContent();
                System.out.println("### \tMessage inside VisualizationAgent " + msgString);
                counter ++;
                guiWindow.updateBoard("", Integer.toString(counter));
            }
            else {
                block();
            }
        }
    }
}
*/