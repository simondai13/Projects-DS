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

import example.*;

//Manages connections to the registry
public class NetworkUtil {

	//hardcoded stub generators 
	public static StringReverse_stub getStringReverseStub(String id, InetSocketAddress objLocation){
		
		return new StringReverse_stub(id, objLocation);
	}
	
	public static RemoteObj getRemoteObject(InetSocketAddress registryLocation, String name){
		return registryLookup(registryLocation,name).localise();
	}
	
	public static RemoteObjectRef registryLookup(InetSocketAddress registryLocation, String name) {

		RegistryRequest req = new RegistryRequest();
		req.type=RegistryRequest.RequestType.LOOKUP;
		req.name = name;
		
		RegistryResponse response = sendRequest(registryLocation, req);
		
		if(response.type == RegistryResponse.ResponseType.OK)
			return response.obj;
		
		return null;
	}
	
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
