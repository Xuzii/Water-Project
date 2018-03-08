package com.amazonaws.ken.drytime;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="WaterProject")
public class WaterData {
	private int ID;
	private String timeStamp;
	private int waterLevel;
	private int valveOpen;
	
	@DynamoDBHashKey(attributeName="ID")
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	@DynamoDBRangeKey(attributeName="currentTime")
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	@DynamoDBAttribute(attributeName="waterLevel") 
	public int getWaterLevel() {
		return waterLevel;
	}
	public void setWaterLevel(int waterLevel) {
		this.waterLevel = waterLevel;
	}
	@DynamoDBAttribute(attributeName="valveOpen") 
	public int getValveOpen() {
		return valveOpen;
	}
	@DynamoDBIgnore
	public void setValveOpen(int valveOpen) {
		this.valveOpen = valveOpen;
	}
	public String toString() {
		return "time: " + this.timeStamp + " waterlevel: " + this.waterLevel + " valveOpen: " + this.valveOpen;
 	}

}
