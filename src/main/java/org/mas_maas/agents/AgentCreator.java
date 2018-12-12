package org.mas_maas.agents;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

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
	
	
    protected void setup() {
        super.setup();
        System.out.println(getAID().getLocalName() + " is ready.");
        this.register("Agent-creator", "JADE-bakery");

        createAgents();
    }
    
 public void createAgents() {
	 
	 getBakery(SMALL_SCENARIO);
	 // Create the equipment for each bakery
	 for (Bakery bakery : bakeries) {
		 
		 // Create a DoughManger per bakery
		 
		 // Create a Baking manager per bakery
		 Vector<Equipment> equipment = bakery.getEquipment();
		 
        System.out.println("Bakery name " + bakery.getName());

        for (int i = 0; i < equipment.size(); i++){
        	
        	if (equipment.get(i) instanceof DoughPrepTable){
        		// Create a DoughPrepTable agent for each DoughPrepTable in the bakery
        	}
        	
            if (equipment.get(i) instanceof Oven){
            	// Create an Oven agent for each Oven in the bakery
            	
                // ovens.add((Oven) equipment.get(i));
            }
        }
        System.out.println("=========================================" );
        System.out.println("Ovens found " + ovens.size());
        System.out.println("=========================================" );
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
