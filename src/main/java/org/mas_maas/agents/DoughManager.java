package org.mas_maas.agents;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mas_maas.messages.KneadingRequest;
import org.mas_maas.messages.PreparationRequest;
import org.mas_maas.objects.Step;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

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

        // For now, the orderProcessingAgents do not exist. The manager has an order object (with the contents of an order message.)
        // Create order message
        String orderMessage;
        JSONObject order = new JSONObject();
        order.put("customer_id","001");
        order.put("order_date","12.04");
        order.put("delivery_date", "13.04");

        JSONArray list_products = new JSONArray();
        JSONObject products =  new JSONObject();
        products.put("Bagel", 2);
        products.put("Berliner", 5);
        list_products.put(products);

        order.put("list_products", list_products);
        orderMessage = order.toString();

        System.out.println(getAID().getName() + " received the order " + orderMessage);

        // Based on the order, fill in a kneadingRequest JSONObject and convert it to string
        // Send the kneadingRequest to the kneadingMachineAgent
        // Once the kneadingMachineAgent is ready, the DoughManager will receive a kneadingNotification

        // Based on the order, fill in a preparationRequest JSONObject and convert it to string
        // Send the preparationRequest to the preparationTableAgent
        // Once the preparationTableAgent is ready, the DoughManager will receive a notitication.
        Gson gson = new Gson();
        //TODO get the actual objects (product type, guids, kneading time)
        String productType = "a product type";
        Vector<String> guids = new Vector<String>();
        guids.add("GUID1");
        guids.add("GUID2");
        Float kneadingTime = (float) 1.0; //TODO maybe these should be doubles
        // Based on the order, fill in a kneadingRequest JSONObject and convert it to string
        // Send the kneadingRequest to the kneadingMachineAgent
        // Once the kneadingMachineAgent is ready, the DoughManager will receive a kneadingNotification
        KneadingRequest kneadingRequest = new KneadingRequest(productType, guids, kneadingTime);
        String kneadingRequestString = gson.toJson(kneadingRequest);
        // TODO send to kneadingMachineAgent

        // TODO get the actual productQuantities and steps
        Float preparationTime = (float) 2.0; // TODO doubles though, right?
        Vector<Integer> productQuantities = new Vector<Integer>();
        productQuantities.add(10);
        productQuantities.add(20);
        Vector<Step> steps = new Vector<Step>();
        steps.add(new Step("doSomething", (float)2.0)); // TODO really starting to think about those Doubles
        steps.add(new Step("doSomethingElse", (float)3.0)); // TODO really starting to think about those Doubles
        // Based on the order, fill in a preparationRequest JSONObject and convert it to string
        // Send the preparationRequest to the preparationTableAgent
        // Once the preparationTableAgent is ready, the DoughManager will receive a notitication.
        PreparationRequest preparationRequest = new PreparationRequest(productType, guids, productQuantities, steps);
        String preparationRequestString = gson.toJson(preparationRequest);

        // Based on the order, fill in a proofingRequest JSONObject and convert it to string
        // Send the proofingnRequest to the ProoferAgent
        // After sending the proofingRequest, the Dough Manager completes the process for the order.

        // Based on the order, fill in a proofingRequest JSONObject and convert it to string
        // Send the proofingnRequest to the ProoferAgent
        // After sending the proofingRequest, the Dough Manager completes the process for the order.



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
