package com.amazonaws.ken.drytime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class AverageDryTime implements RequestHandler<Object, Boolean> {

    
    public static final String URL_DARKSKY_WEATHER_SAMMAMISH =
    		"https://api.darksky.net/forecast/2141d25f35656aa0c44a7fb584653cdc/47.6163,-122.0356";
	
	@Override
    public Boolean handleRequest(Object input, Context context) {
        context.getLogger().log("Input: " + input);
        
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    	DynamoDBMapper mapper = new DynamoDBMapper(client);
    	
        List<DryData> queryResult = mapper.query(DryData.class, dryTimeQuery());
        ArrayList<Long> dryTimes = new ArrayList<Long>();
        ArrayList<Long> valveOpenTimes = new ArrayList<Long>();
        for(DryData dryData : queryResult) {
        	dryTimes.add(dryData.getDryTime());
        }
        for(DryData timeStamp : queryResult) {
        	Date formattedDate;
			try {
				formattedDate = dateFormatter().parse(timeStamp.getTimeStamp());
				long minToHour = formattedDate.getHours() * 60;
	        	long minute = formattedDate.getMinutes();
	        	long totalTime = minToHour + minute;
				valveOpenTimes.add(totalTime);
			} catch (ParseException e) {
				e.printStackTrace();
			}
        }
        Collections.sort(dryTimes);
        Collections.sort(valveOpenTimes);

        //System.out.println("Average Dry Time: " + averageDryTime + " | Average Valve Time: " + averageValveTime);
        
        HashMap<String, Double> rainChance = null;
		try {
			rainChance = chanceOfRain(httpConnection());
			System.out.println(rainChance.get("Hour"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!queryResult.isEmpty()) {
			long averageDryTime = bestTime(dryTimes);
	        long averageValveTime = bestTime(valveOpenTimes);
	        System.out.println("Average Dry Time: " + averageDryTime + " | Average Valve Time: " + averageValveTime);
			if (queryResult.size() < 7) {
	        	return false;
	        } else if(currentClockTime() < averageValveTime + 30 && currentClockTime() > averageValveTime - 30) {
	        	if(queryResult.get(queryResult.size() - 1).getDryTime() > averageDryTime - 60000) {
	        		if(rainChance.get("Hour") > .70) {
	        			return true;
	        		} else {
	        			return false;
	        		}
	        	} else {
	        		return false;
	        	}
	        	
	        } else {
	        	return false;
	        }
		} else {
			return false;
		}
    }
    /* SETUP QUERY FOR DRY DATA*/
    public DynamoDBQueryExpression<DryData> dryTimeQuery(){
    	Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withN("1111"));
        eav.put(":val2", new AttributeValue().withS("Done"));
        
        DynamoDBQueryExpression<DryData> dryTimeQueryExpression = new DynamoDBQueryExpression<DryData>()
        		.withKeyConditionExpression("ID = :val1")
				.withFilterExpression("statusOfProcess = :val2")
				.withExpressionAttributeValues(eav);
        return dryTimeQueryExpression;
    }
    /* DATE FORMATTER */
    public SimpleDateFormat dateFormatter() {
    	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormatter;
    }
    /* BOX AND WHISKER BASED SORTING ALGORITHIM*/
    public long bestTime(ArrayList<Long> dataSet) {
    	long q1 = 0; 
    	long q3 = 0;
        if(dataSet.size()/2 != 0) {
        	q1 = (dataSet.get(dataSet.size()/2/2 - 1) + dataSet.get(dataSet.size()/2/2))/2;
        	q3 = (dataSet.get(dataSet.size()/2/2 + dataSet.size()/2 + 1) + dataSet.get(dataSet.size()/2/2 + dataSet.size()/2 + 2))/2;
        }
        else if(dataSet.size()/2 == 0) {
        	q1 = dataSet.get(dataSet.size()/2/2);
        	q3 = dataSet.get(dataSet.size()/2 + dataSet.size()/2/2);
        }
        long totalTime = 0;
        int numberOfValues = 0;
        for(Long timeValue : dataSet) {
        	if(timeValue > q1 && timeValue < q3) {
        		totalTime += timeValue;
        		numberOfValues++;
        	}
        }
        return totalTime/numberOfValues;
    	
    }
    /* CURRENT TIME */
    public long currentClockTime() {
    	long currentTime = (new Date()).getTime();
    	Date currentDate = new Date();
    	currentDate.setTime(currentTime);
    	int totalMinute = currentDate.getHours() * 60 + currentDate.getMinutes();
		return totalMinute;
    }
    /* CONNECT TO HTTP */
    public String httpConnection() {
    	String result = "";
    	try {
			URL url = new URL(URL_DARKSKY_WEATHER_SAMMAMISH);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader, 8192);
				String line = null;
				while((line = bufferedReader.readLine()) != null) {
					result += line;
				}
				bufferedReader.close();
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return result;
    }
    /* FIND THE AVERAGE CHANCE OF RAIN VIA API */
    private HashMap<String, Double> chanceOfRain(String json) throws JSONException{
    	HashMap<String, Double> precipLevels = new HashMap<String, Double>();
    	JSONObject jsonObject = new JSONObject(json);
    	double precipMinProb = 0;
    	double precipHourProb = 0;
    	JSONObject getJsonMinutely = jsonObject.getJSONObject("minutely");
    	JSONArray getJsonDataM = getJsonMinutely.getJSONArray("data");
    	for (int i = 0; i < getJsonDataM.length(); i++) {
    		JSONObject getJsonDataObject = getJsonDataM.getJSONObject(i);
    		precipMinProb += getJsonDataObject.getDouble("precipProbability");
    	}
    	precipLevels.put("Hour", precipMinProb/getJsonDataM.length());
    	JSONObject getJsonHourly = jsonObject.getJSONObject("hourly");
    	JSONArray getJsonDataH = getJsonHourly.getJSONArray("data");
    	for (int i = 1; i < 5; i++) {
    		JSONObject getJsonDataObject = getJsonDataH.getJSONObject(i);
    		precipHourProb += getJsonDataObject.getDouble("precipProbability");
    	}
    	precipLevels.put("4Hours", precipHourProb/4.0);
    	return precipLevels;
    }
}
