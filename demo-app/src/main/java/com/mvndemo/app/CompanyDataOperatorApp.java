package com.mvndemo.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.demopackage.app.CSVGenerator;


/**
 * Creates, stores and operates with
 * company data
 *
 */
public class CompanyDataOperatorApp
{
    public static void main( String[] args )
    {
        String jsonData = "";
        Path file = Paths.get(args[0]);
        try (InputStream in = Files.newInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                jsonData = jsonData + line;
            }
            JsonReader jr = Json.createReader(new StringReader(jsonData));
            JsonObject jo = jr.readObject();
            JsonArray ja = jo.getJsonArray("data");
            System.out.println("Test");
            CSVGenerator.generateCSV(ja, args[1]);
            jr.close();
        } catch (IOException x) {
            System.err.println(x);
        }
    }
}
