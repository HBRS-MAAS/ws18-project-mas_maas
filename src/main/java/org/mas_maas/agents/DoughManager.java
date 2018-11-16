package org.mas_maas.agents;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mas_maas.JSONConverter;
import org.mas_maas.messages.KneadingRequest;
import org.mas_maas.objects.BakedGood;
import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.Order;
import org.mas_maas.objects.Product;
import org.mas_maas.objects.ProductStatus;
import org.mas_maas.objects.Step;
import org.mas_maas.objects.WorkQueue;

import com.google.gson.Gson;

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

        // Based on the order, fill in a proofingRequest JSONObject and convert it to string
        // Send the proofingnRequest to the ProoferAgent
        // After sending the proofingRequest, the Dough Manager completes the process for the order.



    }

    public void queueOrder(Order order) {
        // Add order to the needKneading workqueue
        for(BakedGood bakedGood : order.getBakedGoods()) {
            String guid = order.getGuid();
            String status = NEED_KNEADING;
            int amount = bakedGood.getAmount();
            Product product = bakery.findProduct(guid);

            ProductStatus productStatus = new ProductStatus(guid, status, amount, product);
            needKneading.addProduct(productStatus);
        }
    }

    public KneadingRequest createKneadingRequestMessage() {
        // Checks the needKneading workqueue
        Vector<ProductStatus> products = needKneading.getProductBatch();

        KneadingRequest kneadingRequest = null;

        if (products != null) {

            Vector<String> guids = new Vector<String>();

            for (ProductStatus productStatus : products) {
                guids.add(productStatus.getGuid());

            }
            String productType = products.get(0).getProduct().getGuid();
            float kneadingTime = products.get(0).getProduct().getRecipe().getActionTime(Step.KNEADING_TIME);

            kneadingRequest = new KneadingRequest(guids, productType, kneadingTime);
        }

        return kneadingRequest;

    }


    public void getbakery(){

        String jsonDir = "src/main/resources/config/dough_stage_communication/";
        try {
            System.out.println("Working Directory = " + System.getProperty("user.dir"));
            String bakeryFile = new Scanner(new File(jsonDir + "bakery.json")).useDelimiter("\\Z").next();
            Vector<Bakery> bakeries = JSONConverter.parseBakeries(bakeryFile);
            for (Bakery bakery : bakeries)
            {
                this.bakery = bakery;
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
