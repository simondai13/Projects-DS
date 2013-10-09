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

//Manages connections to the registry
public class NetworkUtil {

	
	public static InetSocketAddress registryLookup(InetSocketAddress registryLocation, String id){

		RegistryRequest req = new RegistryRequest();
		req.type=RegistryRequest.RequestType.LOOKUP;
		req.id = 0;//CHANGE THIS WHEN WE CHANGE IDS TO STRINGS (id)
		
		RegistryResponse response = sendRequest(registryLocation, req);
		
		return response.address;
	}
	
	public static void registryRegister(InetSocketAddress registryLocation, InetSocketAddress objLocation, String id){
		
		boolean idUnique = true;
		while(idUnique){
			RegistryRequest req = new RegistryRequest();
			req.type = RegistryRequest.RequestType.REGISTER;
			req.address = objLocation;
			req.id = 0;//CHANGE THIS
			
			RegistryResponse resp = sendRequest(registryLocation, req);
			if(resp.type == RegistryResponse.ResponseType.OK)
				idUnique = false;
		}
	}
	
	public static void registryUnregister(InetSocketAddress registryLocation, String id){
		
		RegistryRequest req = new RegistryRequest();
		req.type = RegistryRequest.RequestType.UNREGISTER;
		req.id = 0;//CHANGE THIS 
		
		sendRequest(registryLocation, req);
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
