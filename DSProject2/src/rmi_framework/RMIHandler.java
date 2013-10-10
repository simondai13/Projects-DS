package rmi_framework;
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

/*An instance of this class is needed on each node that contains Objects that need referencing 
 * across the system.  This alongside the RemoteObj interface are the only 2 classes needed by the user.  
 */
public class RMIHandler implements Runnable{

	private ServerSocket server;
	private Map<String, RemoteObj> localObjects;
	private InetSocketAddress registry;
	private InetSocketAddress localHost;
	
	public RMIHandler(InetSocketAddress registry, int port) throws IOException{
		
		server = new ServerSocket(port);
		localHost=new InetSocketAddress(InetAddress.getLocalHost(),server.getLocalPort());
		this.registry=registry;
		localObjects = new HashMap<String, RemoteObj>();
	}
	
	//Returns a remote object stub corresponding to the remote object at the given registry with name
	//name (Static Function)
	public static RemoteObj getRemoteObject(InetSocketAddress registryLocation, String name){
		RemoteObjectRef r =  NetworkUtil.registryLookup(registryLocation,name);
		if (r!=null)
			return r.localise(registryLocation);
		
		return null;
	}
	
	//Registers a remote object r on the registry as well as adding
	//the id to the list of local objects
	public boolean registerObject(RemoteObj r, Class<?> remoteObjType)
	{
		//if id in use, return false
		RemoteObjectRef obj = new RemoteObjectRef(localHost,r.getRMIName(),remoteObjType);
		if(!NetworkUtil.registryRegister(registry, obj))
			return false;
		
		//Store a local reference to the actual object
		localObjects.put(obj.name, r);
		return true;
	}
	
	//Unregister the remote object that was registered with this RMIhandler.  If the name is not 
	//registered locally or on the server, returns false.  Otherwise, returns true
	public boolean unregisterObject(RemoteObj r)
	{
		if(!localObjects.containsKey(r.getRMIName()))
			return false;
		localObjects.remove(r.getRMIName());
		return NetworkUtil.registryUnregister(registry, r.getRMIName());
		
	}
	
	
	//Nested class to handle concurrent RMI requests
	private class ConnectionHandler implements Runnable{

		private Socket client;
		
		public ConnectionHandler(Socket s){
			client = s;
		}
		
		//Handles a single request to an object registered on this RMI handler
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
				
				if(msg.getMessageType() == RMIMessage.RMIMessageType.INVOKE){
					
					//invoke method with arguments.
					
					//get the local object instance given the ID
					String objID= msg.getOwnerID();
					Object obj = localObjects.get(objID);
					if(obj == null){
						
						//Object not on localhost
						return;
					}
					
					String methodName = msg.getMethodName();

					//get the arguments
					Object[] arguments = (Object[])msg.getArguments();
					//check to see if an argument is a ROR, if it is, then we convert it to a stub.
					
					if(arguments!=null){
						for(int i = 0; i < arguments.length; i++){
							//Remote Parameter, converts to stub
							if(RemoteObjectRef.class.isAssignableFrom(arguments[i].getClass())){
								arguments[i] = ((RemoteObjectRef)arguments[i]).localise(registry);
							}
							//Otherwise, we just use the copied object
						}
					}
					
					Method method = null;
					Object toReturn = null;
					try {

						method = obj.getClass().getMethod(methodName, msg.getParamTypes());
						
						//we lock the object we are invoking
						synchronized(obj){
							toReturn = method.invoke(obj, arguments);
						}
					} catch (NoSuchMethodException e){
						System.out.println("Method not found");
					} catch (SecurityException e1) {
						System.out.println("Security Exception");
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {

						//method throws an exception, write exception to client
						Object[] returnArgs = new Object[1];
						returnArgs[0]=e.getCause();
						RMIMessage exceptionMessage = new RMIMessage
								(RMIMessage.RMIMessageType.EXCEPTION, returnArgs,null,methodName, msg.getParamTypes());
						objOut.writeObject(exceptionMessage);
						return;
					} 
					//return the return value
					Object[] returnArgs = new Object[1];
					if(toReturn != null && RemoteObj.class.isAssignableFrom(toReturn.getClass())){
							RemoteObj r = (RemoteObj) toReturn;
							RemoteObjectRef returnRef = NetworkUtil.registryLookup(registry,r.getRMIName());
							returnArgs[0]=returnRef;
					}
					else{
						returnArgs[0]=toReturn;
					}
					RMIMessage returnMessage = 
							new RMIMessage(RMIMessage.RMIMessageType.RETURN,returnArgs, null, methodName, msg.getParamTypes());
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
	
	//Simply wait for connections, and process RMI requests
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

}
