package org.mas_maas.tests;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Vector;

import org.mas_maas.objects.Bakery;
import org.mas_maas.objects.Batch;
import org.mas_maas.objects.DoughPrepTable;
import org.mas_maas.objects.KneadingMachine;
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
    public static void main(String argv[])
    {
        try {
            String JSONFile;
            System.out.println("Working Directory = " + System.getProperty("user.dir"));
            JSONFile = new Scanner(new File("src/main/resources/config/sample/bakeries.json")).useDelimiter("\\Z").next();
            System.out.println(JSONFile);

            JsonElement root = new JsonParser().parse(JSONFile);
            JsonArray arr = root.getAsJsonArray();

            for (JsonElement element : arr)
            {
                // bakery
                JsonObject json_bakery = element.getAsJsonObject();
                String guid = json_bakery.get("guid").getAsString();
                String name = json_bakery.get("name").getAsString();
                JsonObject location = (JsonObject) json_bakery.get("location");
                Double x = location.get("x").getAsDouble();
                Double y = location.get("y").getAsDouble();
                Bakery bakery = new Bakery(guid, name, x, y);
                System.out.println(bakery.toString());

                // products
                JsonArray products = json_bakery.get("products").getAsJsonArray();
                for (JsonElement product : products)
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
                    System.out.println(aProduct.toString());
                }


                // equipment
                JsonObject equipment = json_bakery.get("equipment").getAsJsonObject();
                JsonArray ovens = equipment.get("ovens").getAsJsonArray();
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
                    System.out.println(anOven.toString());
                }

                JsonArray doughPrepTables = equipment.get("doughPrepTables").getAsJsonArray();
                for (JsonElement table : doughPrepTables)
                {
                    JsonObject json_table = table.getAsJsonObject();
                    String table_guid = json_table.get("guid").getAsString();

                    DoughPrepTable aTable = new DoughPrepTable(table_guid);
                    System.out.println(aTable.toString());
                }

                JsonArray kneadingMachines = equipment.get("kneadingMachines").getAsJsonArray();
                for (JsonElement kneadingMachine : kneadingMachines)
                {
                    JsonObject json_kneadingMachine = kneadingMachine.getAsJsonObject();
                    String kneadingMachine_guid = json_kneadingMachine.get("guid").getAsString();

                    KneadingMachine aMachine = new KneadingMachine(kneadingMachine_guid);
                    System.out.println(aMachine.toString());
                }
            }



        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}