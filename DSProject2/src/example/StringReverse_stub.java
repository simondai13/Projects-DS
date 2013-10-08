package example;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import rmi_framework.RMIMessage;



//marshalls RMIMessages
public class StringReverse_stub {

	private String objectID;
	private InetAddress objLocation;
	private int objPort;
	
	public StringReverse_stub(){
		
		objectID = "0";
		try {
			objLocation = InetAddress.getByName("");
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}
		objPort = 5545;
	}
	
	
	public String reverse(String s, Integer numToRev)throws Throwable{
		
		Object[] arguments = new Object[2];
		arguments[0] = s;
		arguments[1] = numToRev;
		
		Class[] argTypes = new Class[2];
		argTypes[0] = s.getClass();
		argTypes[1] = numToRev.getClass();
		
		Object result = handleConnection("reverse", arguments, argTypes);
		if(result == null)
			return null;
		return (String)result;
	}
	
	public String getAppendage() throws Throwable{
		Object[] arguments = new Object[0];
		Class[] argTypes = new Class[0];
		
		Object result = handleConnection("getAppendage", arguments, argTypes);
		if(result == null)
			return null;
		return (String)result;
	}
	
	public void setAppendage(String newApp)throws Throwable{
		
		Object[] arguments = new Object[1];
		arguments[0] = newApp;
		
		Class[] argTypes = new Class[1];
		argTypes[0] = newApp.getClass();

		Object result = handleConnection("setAppendage", arguments, argTypes);
		
	}
	
	//the most abstract I could make it right now
	//the code in this function is universal across every method
	private Object handleConnection(String methodName, Object[] arguments,Class[] argTypes)throws Throwable{

		RMIMessage msg = new RMIMessage(RMIMessage.RMIMessageType.INVOKE, arguments, objectID, "reverse", argTypes);
		
		RMIMessage response = null;
		try {
			Socket client = new Socket(objLocation, objPort);

			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);

			InputStream in = client.getInputStream();
			ObjectInput objIn = new ObjectInputStream(in);
			//Forward the request
			
			objOut.writeObject(msg);
			response = (RMIMessage) objIn.readObject();
			
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		RMIMessage.RMIMessageType messageType = response.getMessageType();
		switch(messageType){
			case RETURN:
				return (String)(response.getArguments());
			case EXCEPTION:
				Exception e = (Exception)(response.getArguments());
				throw e.getCause();
			default://shouldn't be reached
		}
		
		//also shouldn't be reached
		return null;
	}
	
}
