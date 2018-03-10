package com.amazonaws.ken.drytime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class AverageDryTime implements RequestHandler<Object, String> {

    @Override
    public String handleRequest(Object input, Context context) {
        context.getLogger().log("Input: " + input);
        
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    	DynamoDBMapper mapper = new DynamoDBMapper(client);
    	
        List<DryData> queryResult = mapper.query(DryData.class, dryTimeQuery());
        ArrayList<Long> dryTimes = new ArrayList<Long>();
        ArrayList<Date> valveOpenTimes = new ArrayList<Date>();
        for(DryData dryData : queryResult) {
        	dryTimes.add(dryData.getDryTime());
        }
        for(DryData timeStamp : queryResult) {
        	Date formattedDate;
			try {
				formattedDate = dateFormatter().parse(timeStamp.getTimeStamp());
				valveOpenTimes.add(formattedDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
        }
        Collections.sort(dryTimes);
        Collections.sort(valveOpenTimes);
        long averageDryTime = bestTime(dryTimes);

        
        
        return averageDryTime + "";
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
        long totalDryTime = 0;
        int numberOfValues = 0;
        for(Long timeValue : dataSet) {
        	if(timeValue > q1 && timeValue < q3) {
        		totalDryTime += timeValue;
        		numberOfValues++;
        	}
        }
        return totalDryTime/numberOfValues;
    	
    }

}
