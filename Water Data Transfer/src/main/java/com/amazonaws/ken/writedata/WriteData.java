package com.amazonaws.ken.writedata;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;
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

public class WriteData implements RequestHandler<RequestData, String> {

	public final int TEST_ID = 1111;
	public final int EMPTY_WATER_LEVEL = 20;
	
    @Override
    public String handleRequest(RequestData input, Context context) {
        //context.getLogger().log("Input: " + input);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        
        String dateNow = currentDate();
        
        WaterData data = new WaterData();
        
        setWaterData(data, dateNow, input);
        
        dryTimeQuery(data, mapper);
        
        mapper.save(data);
        
        return "time: " + dateNow.toString() + 
        		" | waterlevel: " + input.getWaterLevel() +
        		" | valveopen: " + input.getValveOpen();
    }
    /* GET CURRENT DATE */
    public String currentDate(){ 
    	long date = (new Date()).getTime();
    	Date tDate = new Date();
    	tDate.setTime(date);
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormatter.format(tDate);
    }
    /* SET WATER DATA*/
    public void setWaterData(WaterData data, String date, RequestData input) { 
    	data.setID(TEST_ID);
        data.setTimeStamp(date);
        data.setWaterLevel(input.getWaterLevel());
        data.setValveOpen(input.getValveOpen());
    }
    /* DRY TIME DATA TRANSFER */
    public void dryTimeQuery(WaterData input, DynamoDBMapper mapper) {
    	try {
    		List<DryData> queryDryResult = mapper.query(DryData.class, dryDataQuery(mapper)); // query dry time data based on limit
    		// SET VALUES
			int waterLevel = input.getWaterLevel(); 
			int valveOpen = input.getValveOpen(); 
			Date parsedWaterTime = dateFormatter().parse(input.getTimeStamp());
			DryData dryData = new DryData();
			
    		if(!queryDryResult.isEmpty()){ // check if there is any item w/ status "In-Progress"
    			
    			mapper.delete(queryDryResult.get(queryDryResult.size() - 1)); // Get rid of recent item to update
    			
    			if(waterLevel <= EMPTY_WATER_LEVEL) { // if water level is less than 20 
    				
    				// FIND TOTAL DRY TIME
    				Date parsedDryTime = dateFormatter().parse(queryDryResult.get(0).getTimeStamp());
					long timeDifference = parsedWaterTime.getTime() - parsedDryTime.getTime();
					long previousDryTime = queryDryResult.get(0).getDryTime();
					long totalDryTime = timeDifference + previousDryTime;
					dryData.setDryTime(totalDryTime);
					
    				if(valveOpen == 0) { // if the valve is still closed continue finding dry time
    					setData(dryData, input);
    					dryData.setStatus("In-Progress");	
        			} else if(valveOpen == 1) { // if not stop finding dry time
        				setData(dryData, input);
    					dryData.setStatus("Done");
        			}
					
    			} else if(waterLevel >= EMPTY_WATER_LEVEL){
    				if(valveOpen == 1 || valveOpen == 0) { // this means that an external source watered the plants 
    					setData(dryData, input);
						dryData.setStatus("Invalid Due to External Source");
    				} 
				}
				
    		} else if(queryDryResult.isEmpty()){ // if there isnt anything in progress make one!
    			if (input.getWaterLevel() <= EMPTY_WATER_LEVEL && input.getValveOpen() == 0) {
					setData(dryData, input);
					dryData.setStatus("In-Progress");
    			} 
			}
		/*
		for(WaterData waterData : queryWaterResult) {
			System.out.println(waterData.toString());
		}
		*/
        		mapper.save(dryData);
		System.out.println( "Data Transferred!");
	}	catch (Exception e) {
		e.printStackTrace(); 
		System.out.println("Something Broke!");
	}
}
    /* FORMAT DATE */
    public SimpleDateFormat dateFormatter() {
    	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormatter;
    }
    /* QUERY DRY DATA */
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
    /* SET DATA */
    public void setData(DryData data, WaterData input) { 
    	data.setID(input.getID());
		data.setTimeStamp(input.getTimeStamp());
    }

}
