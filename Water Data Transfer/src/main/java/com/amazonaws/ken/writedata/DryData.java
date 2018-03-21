package com.amazonaws.ken.writedata;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="DryDataProject")
public class DryData {
	private int ID;
	private String timeStamp;
	private long dryTime;
	private String status;
	
	@DynamoDBHashKey(attributeName = "ID")
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	@DynamoDBRangeKey(attributeName = "timeValveTurnsOn")
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}
	@DynamoDBAttribute(attributeName = "dryTime")
	public long getDryTime() {
		return dryTime;
	}
	public void setDryTime(long dryTime) {
		this.dryTime = dryTime;
	}
	@DynamoDBAttribute(attributeName = "statusOfProcess")
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
}
