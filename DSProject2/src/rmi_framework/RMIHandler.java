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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


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
					for(int i = 0; i < arguments.length; i++){
						//Remote Parameter, converts to stub
						if(RemoteObjectRef.class.isAssignableFrom(arguments[i].getClass())){
							
							arguments[i] = ((RemoteObjectRef)arguments[i]).localise(registry);
						}
						//Otherwise, we just use the copied object
					}
					
					Method method = null;
					Object toReturn = null;
					try {

						System.out.println("[RMIHandler] methodName: " + methodName);
						System.out.println("[RMIHandler] objectType: " + obj.getClass());
						System.out.println("[RMIHandler] paramTypes: " + msg.getParamTypes()[0].getName());
						
						method = obj.getClass().getMethod(methodName, msg.getParamTypes());
						
						toReturn = method.invoke(obj, arguments);
					} catch (NoSuchMethodException | SecurityException e1) {

						//FAILURE IN GETTING METHOD
						System.out.println("Method not found");
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
						//FAILURE IN INVOKING METHOD
						e2.printStackTrace();
					} catch (Exception e){
						//method throws an exception, write exception to client
						Object[] returnArgs = new Object[1];
						returnArgs[0]=e;
						RMIMessage exceptionMessage = new RMIMessage
								(RMIMessage.RMIMessageType.EXCEPTION, returnArgs,null,methodName, msg.getParamTypes());
						objOut.writeObject(exceptionMessage);
						return;
					}
					//return the return value
					Object[] returnArgs = new Object[1];
					returnArgs[0]=toReturn;
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
