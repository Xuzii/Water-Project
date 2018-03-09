package com.amazonaws.ken.drytime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        int totalDryTime = 0;
        int numberOfValues = 0;
        long q1 = 0;
        long q3 = 0;
        
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    	DynamoDBMapper mapper = new DynamoDBMapper(client);

        List<DryData> queryResult = mapper.query(DryData.class, dryTimeQuery());
        ArrayList<Long> dryTimes = new ArrayList<Long>();
        
        for(DryData dryData : queryResult) {
        	dryTimes.add(dryData.getDryTime());
        }
        Collections.sort(dryTimes);

        
        //long max = dryTimes.get(dryTimes.size()-1);
        //long min = dryTimes.get(0);
        //test
        if(dryTimes.size()/2 != 0) {
        	//long median = dryTimes.get(dryTimes.size()/2);
        	q1 = (dryTimes.get(dryTimes.size()/2/2 - 1) + dryTimes.get(dryTimes.size()/2/2))/2;
        	q3 = (dryTimes.get(dryTimes.size()/2/2 + dryTimes.size()/2 + 1) + dryTimes.get(dryTimes.size()/2/2 + dryTimes.size()/2 + 2))/2;
        }
        
        for(Long timeValue : dryTimes) {
        	if(timeValue > q1 && timeValue < q3) {
        		totalDryTime += timeValue;
        		numberOfValues++;
        	}
        }
        
        // 1 2 3 4 5 6 7 8 9 
        
        return "";
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

}
