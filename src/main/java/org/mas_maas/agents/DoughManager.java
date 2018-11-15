package org.mas_maas.agents;
import java.util.*;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.DFService;

public class DoughManager extends BaseAgent {
	private AID [] orderProcessingAgents;
	private AID [] prooferAgents;
	private AID [] preparationTableAgents;
	private AID [] kneadingMachineAgents;
	
	protected void setup() {
		super.setup();
		System.out.println(getAID().getLocalName() + " is ready.");
		this.register("Dough-manager", "JADE-bakery");
		this.getOrderProcessingAIDs();
		this.getProoferAIDs();
		this.getPreparationTableAIDS();
		this.getKneadingMachineAIDs();
		
	}
		
	public void getOrderProcessingAIDs() {
		DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Order-processing");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Order-processing agents:");
            orderProcessingAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
            	orderProcessingAgents[i] = result[i].getName();
                System.out.println(orderProcessingAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }	
	}
	
	public void getProoferAIDs() {
		DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Proofer");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Proofer agents:");
            prooferAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
                prooferAgents[i] = result[i].getName();
                System.out.println(prooferAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }	
	}
	
	public void getPreparationTableAIDS() {
		DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Preparation-table");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Preparation-table agents:");
            preparationTableAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
            	preparationTableAgents[i] = result[i].getName();
                System.out.println(preparationTableAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }	
	}
	
	public void getKneadingMachineAIDs() {
		DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        sd.setType("Kneading-machine");
        template.addServices(sd);
        try {
            DFAgentDescription [] result = DFService.search(this, template);
            System.out.println("Found the following Kneading-machine agents:");
            kneadingMachineAgents = new AID [result.length];

            for (int i = 0; i < result.length; ++i) {
            	kneadingMachineAgents[i] = result[i].getName();
                System.out.println(kneadingMachineAgents[i].getName());
            }

        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }	
	}
	



}
