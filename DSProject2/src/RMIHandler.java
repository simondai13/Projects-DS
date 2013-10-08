import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class RMIHandler implements Runnable{

	private ServerSocket server;
	private Map<Long, RemoteObj> localObjects;
	private InetSocketAddress registry;
	private InetSocketAddress localHost;
	
	public RMIHandler(int port, InetSocketAddress registry) throws IOException{
		
		server = new ServerSocket(port);
		localHost=new InetSocketAddress(InetAddress.getLocalHost(),server.getLocalPort());
		this.registry=registry;
		localObjects = new HashMap<Long, RemoteObj>();
	}
	
	//Registers a remote object r on the server as well as adding
	//the id to the list of local objects
	public RemoteObjectRef registerObject(RemoteObj r)
	{
		RegistryRequest req = new RegistryRequest();
		req.type=RegistryRequest.RequestType.REGISTER;
		req.address=localHost;

		RegistryResponse resp = sendRequest(req);
		RemoteObjectRef ref = new RemoteObjectRef(localHost,resp.id);
		localObjects.put(resp.id, r);
		return ref;
	}
	
	
	//handles connections concurrently
	private class ConnectionHandler implements Runnable{

		private Socket client;
		
		public ConnectionHandler(Socket s){
			client = s;
		}
		
		@Override
		public void run() {

			OutputStream out;
			try {
				out = client.getOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(out);
				InputStream in = client.getInputStream();
				ObjectInput objIn = new ObjectInputStream(in);
				Object messageObj = objIn.readObject();

				if(!RMIMessage.class.isAssignableFrom(messageObj.getClass())){
					return;
				}
				RMIMessage msg = (RMIMessage) messageObj;
				
				if(msg.getMessageType().equals("INVOKE")){
					
					//invoke method with arguments.
					
					//get the local object instance given the ID
					String objID= msg.getOwnerID();
					Object obj = localObjects.get(objID);
					if(obj == null){
						
						//Object not on localhost
						return;
					}
					
					String methodName = msg.getMethodName();
					Class[] paramTypes = msg.getParamTypes(); 

					//get the arguments
					Object[] arguments = (Object[])msg.getArguments();
					//check to see if an argument is a ROR, if it is, then we convert it to a stub.
					for(int i = 0; i < arguments.length; i++){
						//Remote Parameter, converts to stub
						if(RemoteObjectRef.class.isAssignableFrom(arguments[i].getClass())){
							
							arguments[i] = ((RemoteObjectRef)arguments[i]).localise();
						}
						//Otherwise, we just use the copied object
					}
					
					Method method = null;
					Object toReturn = null;
					try {
						method = obj.getClass().getMethod(methodName, paramTypes);
						toReturn = method.invoke(obj, arguments);
					} catch (NoSuchMethodException | SecurityException e1) {

						//FAILURE IN GETTING METHOD
						System.out.println("Method not found");
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
						//FAILURE IN INVOKING METHOD
						e2.printStackTrace();
					} catch (Exception e){
						//method throws an exception, write exception to client
						RMIMessage exceptionMessage = new RMIMessage("EXCEPTION", e, null, methodName, paramTypes);
						objOut.writeObject(exceptionMessage);
						return;
					}
					//return the return value
					RMIMessage returnMessage = new RMIMessage("RETURN", toReturn, null, methodName, paramTypes);
					objOut.writeObject(returnMessage);
				}
				else {System.out.println("BAD REQUEST"); return;}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {

		while(true){
			
			try {
				Socket clientConnection = server.accept();
				ConnectionHandler ch = new ConnectionHandler(clientConnection);
				Thread t = new Thread(ch);
				t.start();
			} catch (IOException e) {
				//ignore failed connections, continue to receive connections
			}
			
		}
	}
	
	//Sends a registry request to the server and returns the response
	private RegistryResponse sendRequest(RegistryRequest req)
	{
		RegistryResponse resp = null;
		try
		{
			Socket client = new Socket(registry.getAddress(),registry.getPort());
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
