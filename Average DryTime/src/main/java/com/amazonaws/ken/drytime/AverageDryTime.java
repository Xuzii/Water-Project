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
import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class AverageDryTime implements RequestHandler<Object, Boolean> {

    
    public static final String URL_OPENWEATHERMAP_WEATHER_SAMMAMISH =
    		"http://api.openweathermap.org/data/2.5/weather?q=Sammamish";
	
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
        long averageDryTime = bestTime(dryTimes);
        long averageValveTime = bestTime(valveOpenTimes);

        System.out.println("Average Dry Time: " + averageDryTime + " | Average Valve Time: " + averageValveTime);
        
        if (queryResult.size() < 7) {
        	return false;
        } else if(currentClockTime() < averageValveTime + 30 && currentClockTime() > averageValveTime - 30) {
        	if(queryResult.get(queryResult.size() - 1).getDryTime() < averageDryTime + 60000 && queryResult.get(queryResult.size() - 1).getDryTime() > averageDryTime - 60000) {
        		return true;
        	}else {
        		return false;
        	}
        	
        }else {
        	return false;
        }
        
    }
    
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
    public SimpleDateFormat dateFormatter() {
    	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormatter;
    }
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
    public long currentClockTime() {
    	long currentTime = (new Date()).getTime();
    	Date currentDate = new Date();
    	currentDate.setTime(currentTime);
    	int totalMinute = currentDate.getHours() * 60 + currentDate.getMinutes();
		return totalMinute;
    }
    public void httpConnection() {
    	String result = "";
    	try {
			URL url = new URL(URL_OPENWEATHERMAP_WEATHER_SAMMAMISH);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader, 8192);
				String line = null;
				while((line = bufferedReader.readLine()) != null) {
					result += line;
				}
				bufferedReader.close();
				String rainChance = chanceOfRain(result);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    private String chanceOfRain(String json) throws JSONException{
    	String rainChance = "";
    	
    	JSONObject jsonObject = new JSONObject(json);
    }
}
