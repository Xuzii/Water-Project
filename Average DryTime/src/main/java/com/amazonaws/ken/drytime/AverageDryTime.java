package com.amazonaws.ken.drytime;

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
        int averageDryTime = 0;
        
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    	DynamoDBMapper mapper = new DynamoDBMapper(client);

        List<DryData> queryResult = mapper.query(DryData.class, dryTimeQuery());
        
        for(DryData dryData : queryResult) {
        	totalDryTime += dryData.getDryTime();
        }
        averageDryTime = totalDryTime / queryResult.size();
        
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

}
