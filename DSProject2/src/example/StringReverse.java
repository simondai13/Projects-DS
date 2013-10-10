package example;

import rmi_framework.RemoteObj;

public interface StringReverse extends RemoteObj{

	public String reverse(String s, Integer numToRev)throws IllegalArgumentException;
	
	public String getAppendage();
	
	public void setAppendage(String newApp);
}
