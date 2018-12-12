package org.mas_maas.agents;
import java.util.Vector;

import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.DoughPrepTable;
import org.mas_maas.objects.Equipment;
import org.mas_maas.objects.KneadingMachine;
import org.mas_maas.objects.Oven;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class BakeryAgent extends BaseAgent {
	// bakery object for this bakery will all the information about the bakery
	private Bakery bakery = null;
	private String doughManagerAgentName;
	private String bakingManagerAgentName;
	private AgentContainer container = null;
	private Vector<Equipment> equipment;


	protected void setup() {
		super.setup();
		
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			System.out.println("IN the arguments");
			this.bakery = (Bakery) args[0];
			this.container = (AgentContainer) args[1];
		}

		System.out.println("Bakery agent created!");
		System.out.println(getAID().getLocalName() + " is ready.");
		System.out.println("Bakery name " + bakery.getName());
		this.register("Bakery", "JADE-bakery");

		// Get equipment for this bakery
		equipment = bakery.getEquipment();

		createManagerAgents();

		createEquipmentAgents();
		
		createInterfaceAgents(); // Proofer and CoolingRack
		
		//

	}
	
	private void createManagerAgents() {
		
		//Create one DoughManager for this Bakery
		doughManagerAgentName = "DoughManagerAgent_" + bakery.getGuid();
		try {
			 Object[] args = new Object[1];
		   	 args[1] = doughManagerAgentName;

			AgentController doughManagerAgent = container.createNewAgent(doughManagerAgentName, "org.mas_maas.agents.DoughManager", args);
			doughManagerAgent.start();

			System.out.println(getLocalName()+" created and started:"+ doughManagerAgent + " on container "+((ContainerController) container).getContainerName());
		} catch (Exception any) {
			any.printStackTrace();
		}
		
		bakingManagerAgentName = "BakingManagerAgent_" + bakery.getGuid();
		try {
			 Object[] args = new Object[1];
		   	 args[1] = bakingManagerAgentName;

			AgentController bakingManagerAgent = container.createNewAgent(bakingManagerAgentName, "org.mas_maas.agents.BakingManager", args);
			bakingManagerAgent.start();

			System.out.println(getLocalName()+" created and started:"+ bakingManagerAgent + " on container "+((ContainerController) container).getContainerName());
		} catch (Exception any) {
			any.printStackTrace();
		}
		
	}
	
	private void createInterfaceAgents() {
		//Create one Proofer agent for this Bakery
		String prooferAgentName = "ProoferAgent_" + bakery.getGuid();
		try {
			 Object[] args = new Object[1];
		   	 args[1] = prooferAgentName;

			AgentController prooferAgent = container.createNewAgent(prooferAgentName, "org.mas_maas.agents.Proofer", args);
			prooferAgent.start();

			System.out.println(getLocalName()+" created and started:"+ prooferAgent + " on container "+((ContainerController) container).getContainerName());
		} catch (Exception any) {
			any.printStackTrace();
		}
		
		// TODO: Add coolingRackAgent
	}
	

	private void createEquipmentAgents() {

		for (int i = 0; i < equipment.size(); i++){
			
			// Create KneadingMachineAgents agents fot this bakery
			if (equipment.get(i) instanceof KneadingMachine){
				
				String kneadingMachineAgentName = equipment.get(i).getGuid();
				
				// Object of type KneadingMachine
				KneadingMachine kneadingMachine = (KneadingMachine) equipment.get(i);

				try {
					 Object[] args = new Object[3];
		        	 args[0] = kneadingMachine;  // kneadingMachine obj
		        	 args[1] = "KneadingMachineAgent_" + bakery.getGuid(); // kneadingMachine name
		        	 args[2] = doughManagerAgentName;

					AgentController kneadingMachineAgent = container.createNewAgent(kneadingMachineAgentName, "org.mas_maas.agents.KneadingMachineAgent", args);
					kneadingMachineAgent.start();

					System.out.println(getLocalName()+" created and started:"+ kneadingMachineAgent + " on container "+((ContainerController) container).getContainerName());
				} catch (Exception any) {
					any.printStackTrace();
				}
			}
			
			
			// Create DougPrepTable agents for this bakery
			if (equipment.get(i) instanceof DoughPrepTable){
				
				String preparationTableAgentName = equipment.get(i).getGuid();
				
				//Object of type DoughPrepTable
				DoughPrepTable doughPrepTable = (DoughPrepTable) equipment.get(i);

				try {
					 Object[] args = new Object[3];
		        	 args[0] = doughPrepTable;
		        	 args[1] = "DoughPrepTableAgent_" + bakery.getGuid();
		        	 args[2] = doughManagerAgentName;

					AgentController preparationTableAgent = container.createNewAgent(preparationTableAgentName, "org.mas_maas.agents.PreparationTableAgent", args);
					preparationTableAgent.start();

					System.out.println(getLocalName()+" created and started:"+ preparationTableAgent + " on container "+((ContainerController) container).getContainerName());
				} catch (Exception any) {
					any.printStackTrace();
				}
			}
			
			// Create DougPrepTable agents for this bakery
			if (equipment.get(i) instanceof Oven){
				
				String OvenAgentName = equipment.get(i).getGuid();
				
				//Object of type Oven
				Oven oven = (Oven) equipment.get(i);

				try {
					 Object[] args = new Object[3];
		        	 args[0] = oven;
		        	 args[1] = "OvenAgent_" + bakery.getGuid();
		        	 args[1] = bakingManagerAgentName;

					AgentController OvenAgent = container.createNewAgent(OvenAgentName, "org.mas_maas.agents.OvenAgent", args);
					OvenAgent.start();

					System.out.println(getLocalName()+" created and started:"+ OvenAgent + " on container "+((ContainerController) container).getContainerName());
				} catch (Exception any) {
					any.printStackTrace();
				}
			}




		}

	}
}