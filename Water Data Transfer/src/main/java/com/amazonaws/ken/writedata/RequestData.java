package com.amazonaws.ken.writedata;

public class RequestData {
	private int ID;
	private int waterLevel;
	private int valveOpen;
	
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	public int getWaterLevel() {
		return waterLevel;
	}
	public void setWaterLevel(int waterLevel) {
		this.waterLevel = waterLevel;
	}
	public int getValveOpen() {
		return valveOpen;
	}
	public void setValveOpen(int valveOpen) {
		this.valveOpen = valveOpen;
	}
}
