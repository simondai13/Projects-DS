package example;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import rmi_framework.RMIMessage;

//marshalls RMIMessages
public class GlobalStringReverse_stub implements GlobalStringReverse{

	private String objectID;
	private InetAddress objLocation;
	private int objPort;

	public GlobalStringReverse_stub(){
		
		objectID = "0";
		try {
			objLocation = InetAddress.getByName("");
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}
		objPort = 5545;
	}
	public GlobalStringReverse_stub(String objectID, InetSocketAddress objectLocation){
		
		this.objectID = objectID;
		objLocation = objectLocation.getAddress();
		objPort = objectLocation.getPort();
	}

	public List<String> globalReverse(List<String> l, StringReverse reverser) throws IndexOutOfBoundsException{

		Object[] arguments = new Object[2];
		arguments[0] = l;
		arguments[1] = reverser;
		
		Class[] argTypes = new Class[2];
		argTypes[0] = l.getClass();
		argTypes[1] = reverser.getClass();
		
		Object result = null;
		try {
			result = handleConnection("globalReverse", arguments, argTypes);
		} catch (Throwable e) {
			throw new IndexOutOfBoundsException(e.getMessage());
		}

		if(result == null)
			return null;
		return (List<String>)result;
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
				return  null;//(String)(response.getArguments());
			case EXCEPTION:
				//Exception e = (Exception)(response.getArguments());
				//throw e.getCause();
			default://shouldn't be reached
		}
		
		//also shouldn't be reached
		return null;
	}
	@Override
	public String getRMIName() {
		return objectID;
	}
	
}
