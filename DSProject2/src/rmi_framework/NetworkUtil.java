package rmi_framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
/*
 * Static Utility Class used to communicate with the registry.  This class is used internally,
 * and does not need to be used by a user
 */
public class NetworkUtil {
	
	//Lookup a remote object at the given registry location, and return a remoteobjectRef if it is a 
	//valid name, or null otherwise
	public static RemoteObjectRef registryLookup(InetSocketAddress registryLocation, String name) {

		RegistryRequest req = new RegistryRequest();
		req.type=RegistryRequest.RequestType.LOOKUP;
		req.name = name;
		
		RegistryResponse response = sendRequest(registryLocation, req);

		if(response.type == RegistryResponse.ResponseType.OK)
			return response.obj;
		
		return null;
	}
	
	//Register remoteObject with the registry
	public static boolean registryRegister(InetSocketAddress registryLocation, RemoteObjectRef obj){
	
		RegistryRequest req = new RegistryRequest();
		req.type = RegistryRequest.RequestType.REGISTER;
		req.obj = obj;
		req.name = obj.name;
		
		RegistryResponse resp = sendRequest(registryLocation, req);
		if(resp.type == RegistryResponse.ResponseType.OK)
			return true;
		return false;
	}
	
	//Remove the given name from the registry index, this remote object will no longer be able 
	//to be located
	public static boolean registryUnregister(InetSocketAddress registryLocation, String name){
		
		RegistryRequest req = new RegistryRequest();
		req.type = RegistryRequest.RequestType.UNREGISTER;
		req.name = name;
		
		RegistryResponse resp = sendRequest(registryLocation, req);
		if(resp.type == RegistryResponse.ResponseType.OK)
			return true;
		return false;
		
	}
	
	//Sends a registry request to the server and returns the response
	public static RegistryResponse sendRequest(InetSocketAddress registryLocation, RegistryRequest req)
	{
		RegistryResponse resp = null;
		try
		{
			Socket client = new Socket(registryLocation.getAddress(),registryLocation.getPort());
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
		
			objOut.writeObject(req);

			InputStream in = client.getInputStream();
			ObjectInput objIn = new ObjectInputStream(in);
			
			resp = (RegistryResponse) objIn.readObject();

			client.close();
			
		} catch (IOException e) {
			System.out.println("Error: Unable to connect to Registry Server");
		} catch (ClassNotFoundException e) {
			System.out.println("Error: Invalid reponse from Registry Server");
		}
		return resp;
	}
}
