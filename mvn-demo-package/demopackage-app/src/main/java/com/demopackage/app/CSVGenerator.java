package com.demopackage.app;

import java.io.File;
import java.io.IOException;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.io.FileUtils;
import org.json.CDL;
import org.json.JSONArray;
/**
 * Generates and stores CSV files based on
 * given data and path
 *
 */
public class CSVGenerator
{
    public static void generateCSV(JsonArray data, String path)
    {
        System.out.println(data);
        JSONArray convertedData = CSVGenerator.convertArrayType(data);
        String csvData = CDL.toString(convertedData);
        System.out.println(csvData);

        File csvFile = new File(path);
        try {
            csvFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.writeStringToFile(csvFile, csvData, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JSONArray convertArrayType(JsonArray array)
    {
        JSONArray convertedData = new JSONArray();
        for (int i = 0; i < array.size(); i++) {
            JsonObject el = array.getJsonObject(i);
            convertedData.put(new org.json.JSONObject(el.toString()));
        }
        return convertedData;
    }
}
