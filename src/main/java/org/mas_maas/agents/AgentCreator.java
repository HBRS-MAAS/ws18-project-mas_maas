package org.mas_maas.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;

import org.mas_maas.JSONConverter;
import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.DoughPrepTable;
import org.mas_maas.objects.Equipment;
import org.mas_maas.objects.Oven;

public class AgentCreator extends BaseAgent {
	private Vector<Bakery> bakeries;
	public static final String SMALL_SCENARIO = "src/main/resources/config/small/";	
	private AgentContainer container = null;
	
    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("Agent-creator", "JADE-bakery");
        
        //get the container controller for creating new agents. All agents will belong to the same container. 
        container = getContainer();
        createAgents();
    }
    
 public AgentContainer getContainer() {
	AgentContainer container = (AgentContainer)getContainerController(); // get a container controller for creating new agents
	return container;
 }
    
 public void createAgents() {
	 
	 getBakery(SMALL_SCENARIO);
	 // Create a BakeryAgent for each bakery
	 for (Bakery bakery : bakeries) {
		 
		 System.out.println("Creating BakeryAgent" + bakery.getGuid());
        
		 // The names of the bakeries are the IDs
         String bakeryAgentName = bakery.getGuid();

         try {
        	 Object[] args = new Object[2];
        	 args[0] = bakery;
        	 args[1] = container;
       
			AgentController bakeryAgent = container.createNewAgent(bakeryAgentName, "org.mas_maas.agents.BakeryAgent", args);
			
			bakeryAgent.start();

			System.out.println(getLocalName()+" created and started:"+ bakeryAgent + " on container "+((ContainerController) container).getContainerName());
         	} catch (Exception any) {
			any.printStackTrace();
         	}
	 }
	 
 }
 
 public void getBakery(String scenarioName){
	 // Select the scenario file to use
	 // guid is the name of the bakery
     String jsonDir = scenarioName; //"src/main/resources/config/small/";
     try {
         // System.out.println("Working Directory = " + System.getProperty("user.dir"));
         String bakeryFile = new Scanner(new File(jsonDir + "bakeries.json")).useDelimiter("\\Z").next();
         this.bakeries = JSONConverter.parseBakeries(bakeryFile);
     } catch (FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
     }
 }
 
 
 
}
