package org.mas_maas.tests;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;

import org.mas_maas.objects.BakedGood;
import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.Batch;
import org.mas_maas.objects.Client;
import org.mas_maas.objects.DoughPrepTable;
import org.mas_maas.objects.Equipment;
import org.mas_maas.objects.KneadingMachine;
import org.mas_maas.objects.Order;
import org.mas_maas.objects.Oven;
import org.mas_maas.objects.Packaging;
import org.mas_maas.objects.Product;
import org.mas_maas.objects.Recipe;
import org.mas_maas.objects.Step;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ParserTester
{
    // this all needs to get cleaned up and broken out but for now this is at
    // least a proof of concept that these .json files and be read and stored
    // in the appropriate objects.
    public static void main(String argv[])
    {
        String jsonDir = "src/main/resources/config/sample/";
        try {
            //System.out.println("Working Directory = " + System.getProperty("user.dir"));

            String bakeryFile = new Scanner(new File(jsonDir + "bakeries.json")).useDelimiter("\\Z").next();
            Vector<Bakery> bakeries = parseBakery(bakeryFile);
            for (Bakery bakery : bakeries)
            {
                System.out.println(bakery);
            }

            String clientFile = new Scanner(new File(jsonDir + "clients.json")).useDelimiter("\\Z").next();
            Vector<Client> clients = parseClient(clientFile);
            for (Client client : clients)
            {
                System.out.println(client);
            }


        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Vector<Bakery> parseBakery(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);
        JsonArray arr = root.getAsJsonArray();

        Vector<Bakery> bakeries = new Vector<Bakery>();
        for (JsonElement element : arr)
        {
            // bakery
            JsonObject json_bakery = element.getAsJsonObject();
            String guid = json_bakery.get("guid").getAsString();
            String name = json_bakery.get("name").getAsString();
            JsonObject json_location = (JsonObject) json_bakery.get("location");
            Double x = json_location.get("x").getAsDouble();
            Double y = json_location.get("y").getAsDouble();
            Point2D location = new Point2D.Double(x,y);

            // products
            Vector<Product> products = new Vector<Product>();
            JsonArray json_products = json_bakery.get("products").getAsJsonArray();
            for (JsonElement product : json_products)
            {
                JsonObject json_product = product.getAsJsonObject();
                String product_guid = json_product.get("guid").getAsString();

                JsonObject json_batch = json_product.get("batch").getAsJsonObject();
                int breadsPerOven = json_batch.get("breadsPerOven").getAsInt();
                Batch batch = new Batch(breadsPerOven);

                JsonObject json_recipe = json_product.get("recipe").getAsJsonObject();
                int coolingRate = json_recipe.get("coolingRate").getAsInt();
                int bakingTemp = json_recipe.get("bakingTemp").getAsInt();

                JsonArray step_array = json_recipe.get("steps").getAsJsonArray();
                Vector<Step> steps = new Vector<Step>();
                for (JsonElement step : step_array)
                {
                    JsonObject json_step = step.getAsJsonObject();
                    String action = json_step.get("action").getAsString();
                    int duration = json_step.get("duration").getAsInt();
                    Step aStep = new Step(action, duration);
                    steps.add(aStep);
                }
                Recipe recipe = new Recipe(coolingRate, bakingTemp, steps);

                JsonObject json_packaging = json_product.get("packaging").getAsJsonObject();
                int boxingTemp = json_packaging.get("boxingTemp").getAsInt();
                int breadsPerBox = json_packaging.get("breadsPerBox").getAsInt();
                Packaging packaging = new Packaging(boxingTemp, breadsPerBox);

                Double salesPrice = json_product.get("salesPrice").getAsDouble();
                Double productionCost = json_product.get("productionCost").getAsDouble();

                Product aProduct = new Product(product_guid, batch, recipe, packaging, salesPrice,  productionCost);
                products.add(aProduct);
            }


            // equipment
            Vector<Equipment> equipment = new Vector<Equipment>();
            JsonObject json_equipment = json_bakery.get("equipment").getAsJsonObject();
            JsonArray ovens = json_equipment.get("ovens").getAsJsonArray();
            for (JsonElement oven : ovens)
            {
                JsonObject json_oven = oven.getAsJsonObject();
                String oven_guid = json_oven.get("guid").getAsString();

                // multiple ways of denoting rates (CamelCase and _ are used)
                // TODO cleanup and make into a function
                int coolingRate = -1;
                if (json_oven.toString().contains("coolingRate"))
                {
                    coolingRate = json_oven.get("coolingRate").getAsInt();
                }
                else if (json_oven.toString().contains("cooling_rate"))
                {
                    coolingRate = json_oven.get("cooling_rate").getAsInt();
                }

                // TODO cleanup and make into a function
                int heatingRate = -1;
                if (json_oven.toString().contains("heatingRate"))
                {
                    heatingRate = json_oven.get("heatingRate").getAsInt();
                }
                else if (json_oven.toString().contains("heating_rate"))
                {
                    heatingRate = json_oven.get("heating_rate").getAsInt();
                }

                Oven anOven = new Oven(oven_guid, coolingRate, heatingRate);
                equipment.add(anOven);
            }

            JsonArray doughPrepTables = json_equipment.get("doughPrepTables").getAsJsonArray();
            for (JsonElement table : doughPrepTables)
            {
                JsonObject json_table = table.getAsJsonObject();
                String table_guid = json_table.get("guid").getAsString();

                DoughPrepTable aTable = new DoughPrepTable(table_guid);
                equipment.add(aTable);
            }

            JsonArray kneadingMachines = json_equipment.get("kneadingMachines").getAsJsonArray();
            for (JsonElement kneadingMachine : kneadingMachines)
            {
                JsonObject json_kneadingMachine = kneadingMachine.getAsJsonObject();
                String kneadingMachine_guid = json_kneadingMachine.get("guid").getAsString();

                KneadingMachine aMachine = new KneadingMachine(kneadingMachine_guid);
                equipment.add(aMachine);
            }

            Bakery bakery = new Bakery(guid, name, location, products, equipment);
            bakeries.add(bakery);
        }

        return bakeries;
    }


    public static Vector<Client> parseClient(String jsonFile)
    {
        JsonElement root = new JsonParser().parse(jsonFile);
        JsonArray arr = root.getAsJsonArray();

        Vector<Client> clients = new Vector<Client>();
        for (JsonElement element : arr)
        {
            JsonObject json_client = element.getAsJsonObject();
            String guid = json_client.get("guid").getAsString();
            int type = json_client.get("type").getAsInt();
            String name = json_client.get("name").getAsString();
            JsonObject json_location = (JsonObject) json_client.get("location");
            Double x = json_location.get("x").getAsDouble();
            Double y = json_location.get("y").getAsDouble();
            Point2D location = new Point2D.Double(x,y);

            // orders
            Vector<Order> orders = new Vector<Order>();
            JsonArray json_orders = json_client.get("orders").getAsJsonArray();
            for (JsonElement order : json_orders)
            {
                JsonObject json_order = order.getAsJsonObject();
                String customerId = json_order.get("customerId").getAsString();
                String order_guid = json_order.get("guid").getAsString();
                JsonObject json_orderDate = json_order.get("orderDate").getAsJsonObject();
                int orderDay = json_orderDate.get("day").getAsInt();
                int orderHour = json_orderDate.get("day").getAsInt();
                JsonObject json_deliveryDate = json_order.get("deliveryDate").getAsJsonObject();
                int deliveryDay = json_deliveryDate.get("day").getAsInt();
                int deliveryHour = json_deliveryDate.get("day").getAsInt();

                // products (BakedGood objects)
                // TODO shouldn't products be an array not an object?
                // TODO this will need to be reworked in the future when BakedGood is more fleshed out
                // TODO also this JUST is a bit hacky...
                Vector<BakedGood> bakedGoods = new Vector<BakedGood>();
                JsonObject json_products = json_order.get("products").getAsJsonObject();
                for (String bakedGoodName : BakedGood.bakedGoodNames)
                {
                    int amount = json_products.get(bakedGoodName).getAsInt();
                    bakedGoods.add(new BakedGood(bakedGoodName, amount));
                }

                Order anOrder = new Order(customerId, order_guid, orderDay, orderHour, deliveryDay, deliveryHour, bakedGoods);
                orders.add(anOrder);
            }

            Client aClient = new Client(guid, type, name, location, orders);
            clients.add(aClient);
        }

        return clients;
    }
}