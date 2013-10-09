package rmi_framework;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;

public class RemoteObjectRegistry implements Runnable {
	private ServerSocket server;
	
	//maps unique remote object names to locations 
	private TreeMap<Long,InetSocketAddress> remoteObjects;
	private long registryCounter;
	
	public RemoteObjectRegistry(int portNum) throws IOException {
		registryCounter = 0;
		remoteObjects = new TreeMap<Long, InetSocketAddress>();
		server=new ServerSocket(portNum);
	}
	
	
	//Run a new instance of this class with each connection to 
	//allow concurrent requests to be serviced 
	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		
		//Handle the life cycle of a remote object query, and then let this thread die
		@Override
		public void run() {
			try{
				 OutputStream out = client.getOutputStream();
				 ObjectOutput objOut = new ObjectOutputStream(out);
				 
				 InputStream in = client.getInputStream();
				 ObjectInput objIn = new ObjectInputStream(in);
				 Object obj = objIn.readObject();
				 
				 //Make sure this message is packed properly
				 if(!RegistryRequest.class.isAssignableFrom(obj.getClass())){
					 return;
				 }
				 RegistryRequest req = (RegistryRequest) obj;
				 RegistryResponse resp = new RegistryResponse();
				 synchronized (remoteObjects)
				 {
					 switch (req.type) {
					 	case REGISTER :
					 		resp.id=registryCounter;
					 		registryCounter++;
					 		
					 		remoteObjects.put(resp.id, req.address);
					 		resp.type = RegistryResponse.ResponseType.OK;
					 		break;
					 	case LOOKUP :
					 		if(!remoteObjects.containsKey(req.id))
					 		{
					 			resp.type = RegistryResponse.ResponseType.NOT_FOUND;
					 		}
					 		else
					 		{
					 			resp.type = RegistryResponse.ResponseType.OK;
					 			resp.address = remoteObjects.get(req.id);
					 		}
					 		break;
					 	case UNREGISTER :
					 		if(!remoteObjects.containsKey(req.id))
					 		{
					 			resp.type = RegistryResponse.ResponseType.NOT_FOUND;
					 		}
					 		else
					 		{
					 			remoteObjects.remove(req.id);
					 			resp.type = RegistryResponse.ResponseType.OK;
					 		}
					 		break;
					 }
				 }
				 
				 //Send the response to the client
				 objOut.writeObject(resp);
				 
			 } catch (ClassNotFoundException e) {
				 System.out.println("Invalid Process Request");
			 }  catch (IOException e) {
				 System.out.println("Error receiving client message.");
			 }
		}
	}
		
	
	@Override
	//Simply accept connections and spawn ConnectionHandles as needed for this client
	public void run() {
		while(true) {
			try {
				Socket client = server.accept();
				//Generate a connection handle and run it in a 
				//separate thread
				ConnectionHandle ch = new ConnectionHandle(client);
				Thread t = new Thread(ch);
				t.start();
				
			} catch (IOException e) {
				// Just chalk it up to a failed connection and keep
				// running
			}
			 
				 
		}
				 
	}
			 

}