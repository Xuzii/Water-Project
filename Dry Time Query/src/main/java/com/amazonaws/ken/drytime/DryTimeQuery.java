package com.amazonaws.ken.drytime;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class DryTimeQuery implements RequestHandler<Object, String> {
	public final int EMPTY_WATER_LEVEL = 20;
    @Override
    public String handleRequest(Object input, Context context) {
        context.getLogger().log("Input: " + input);
    	AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    	DynamoDBMapper mapper = new DynamoDBMapper(client);
    	
    	String dateOneWeekAgoF = dateOneWeekAgo();
    	
    	try {
    		List<WaterData> queryWaterResult = mapper.query(WaterData.class, waterDataQuery(dateOneWeekAgoF, mapper));
    		List<DryData> queryDryResult = mapper.query(DryData.class, dryDataQuery(mapper));
    		
    		if(!queryWaterResult.isEmpty()){
    			
    				int waterLevel = queryWaterResult.get(queryWaterResult.size() - 1).getWaterLevel();
    				int valveOpen = queryWaterResult.get(queryWaterResult.size() - 1).getValveOpen();
    				Date parsedWaterTime = dateFormatter().parse(queryWaterResult.get(queryWaterResult.size() - 1).getTimeStamp());
    				DryData dryData = new DryData();
    				
            		if(!queryDryResult.isEmpty()){
            			
            			mapper.delete(queryDryResult.get(queryDryResult.size() - 1));
            			
            			if(waterLevel <= EMPTY_WATER_LEVEL) {
            				
            				Date parsedDryTime = dateFormatter().parse(queryDryResult.get(0).getTimeStamp());
        					long timeDifference = parsedWaterTime.getTime() - parsedDryTime.getTime();
        					long previousDryTime = queryDryResult.get(0).getDryTime();
        					long totalDryTime = timeDifference + previousDryTime;
        					dryData.setDryTime(totalDryTime);
        					
            				if(valveOpen == 0) {
            					setData(dryData, queryWaterResult);
            					dryData.setStatus("In-Progress");	
                			} else if(valveOpen == 1) {
                				setData(dryData, queryWaterResult);
            					dryData.setStatus("Done");
                			}
        					
            			} else if(waterLevel >= EMPTY_WATER_LEVEL){
    						setData(dryData, queryWaterResult);
    						dryData.setStatus("Invalid Due to External Source");
        				}
        				
            		} else if(queryDryResult.isEmpty()){
            			if (queryWaterResult.get(queryWaterResult.size()-1).getValveOpen() == 0) {
    						setData(dryData, queryWaterResult);
    						dryData.setStatus("In-Progress");
            			}
    			}
    		/*
    		for(WaterData waterData : queryWaterResult) {
    			System.out.println(waterData.toString());
    		}
    		*/
            		mapper.save(dryData);
    		}
    		return "Data Transferred!";
    	}
    	
    	catch (Exception e) {
    		e.printStackTrace(); 
    		return "Something Broke!";
    	}
    	
    }
    public SimpleDateFormat dateFormatter() {
    	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormatter;
    }
    public String dateOneWeekAgo() {
    	long timeOneWeekAgo = (new Date()).getTime() - (8L * 24L * 60L * 60L * 1000L);
    	Date dateOneWeekAgo = new Date();
    	dateOneWeekAgo.setTime(timeOneWeekAgo);
		return dateFormatter().format(dateOneWeekAgo);
    }
    public DynamoDBQueryExpression<WaterData> waterDataQuery(String dateOneWeekAgoF, DynamoDBMapper mapper) {
    	Map<String, AttributeValue> eavWater = new HashMap<String, AttributeValue>();
		eavWater.put(":val1", new AttributeValue().withN("1111"));
		eavWater.put(":val2", new AttributeValue().withS(dateOneWeekAgoF.toString()));
		//eavWater.put(":val3", new AttributeValue().withN(Integer.toString(EMPTY_WATER_LEVEL)));

		DynamoDBQueryExpression<WaterData> queryWaterExpression = new DynamoDBQueryExpression<WaterData>()
				.withKeyConditionExpression("ID = :val1 and currentTime > :val2")
				//.withFilterExpression("waterLevel <= :val3")
				.withExpressionAttributeValues(eavWater);
		return queryWaterExpression;
    }
    public DynamoDBQueryExpression<DryData> dryDataQuery(DynamoDBMapper mapper) {
    	Map<String, AttributeValue> eavDry = new HashMap<String, AttributeValue>();
		eavDry.put(":val1", new AttributeValue().withN("1111"));
		eavDry.put(":val2", new AttributeValue().withS("In-Progress"));
		
		DynamoDBQueryExpression<DryData> queryDryExpression = new DynamoDBQueryExpression<DryData>()
				.withKeyConditionExpression("ID = :val1")
				.withFilterExpression("statusOfProcess = :val2")
				.withExpressionAttributeValues(eavDry);
		return queryDryExpression;
    }
    public void setData(DryData data, List<WaterData> queryResult) {
    	data.setID(queryResult.get(0).getID());
		data.setTimeStamp(queryResult.get(queryResult.size()-1).getTimeStamp());
    }

}
