package com.amazonaws.ken.writedata;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Date;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class writeData implements RequestHandler<RequestData, String> {

	public final int TEST = 1111;
    @Override
    public String handleRequest(RequestData input, Context context) {
        //context.getLogger().log("Input: " + input);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        
        String dateNow = currentDate();
        
        WaterData data = new WaterData();
        
        setWaterData(data, dateNow, input);
        
        mapper.save(data);
        
        return "time: " + dateNow.toString() + 
        		" | waterlevel: " + input.getWaterLevel() +
        		" | valveopen: " + input.getValveOpen();
    }
    
    public String currentDate(){
    	long date = (new Date()).getTime();
    	Date tDate = new Date();
    	tDate.setTime(date);
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormatter.format(tDate);
    }
    
    public void setWaterData(WaterData data, String date, RequestData input) {
    	data.setID(TEST);
        data.setTimeStamp(date);
        data.setWaterLevel(input.getWaterLevel());
        data.setValveOpen(input.getValveOpen());
    }

}
