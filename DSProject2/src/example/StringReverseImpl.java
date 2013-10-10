package example;
import rmi_framework.RemoteObj;

public class StringReverseImpl implements RemoteObj, StringReverse{

	
	private String appendage;
	public String name;

	public StringReverseImpl(String app){
		
		if(app == null)
			appendage = "";
		else
			appendage = app;
	}
	public StringReverseImpl(String app, String name){
		
		if(app == null)
			appendage = "";
		else
			appendage = app;
		this.name = name;
	}
	
	public String reverse(String s, Integer numToRev)throws IllegalArgumentException{
		
		if(numToRev > s.length())
			throw new IllegalArgumentException("invalid number");
			
		String toReturn = (new StringBuffer(s.substring(0, numToRev))).reverse().toString() + s.substring(numToRev) + appendage;
		
		return toReturn;
	}
	
	public String getAppendage(){
		
		return appendage;
	}
	
	public void setAppendage(String newApp){
		if(newApp == null)
			appendage = "";
		else
			appendage = newApp;
	}
	@Override
	public String getRMIName() {

		return name;
	}
}
