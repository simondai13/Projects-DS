package rmi_framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RMIStubInvocationHandler implements InvocationHandler {

	private InetSocketAddress registryLocation;
	private InetSocketAddress objLocation;
	private String name;
	
	public RMIStubInvocationHandler(InetSocketAddress registryLocation, String name){
		this.registryLocation=registryLocation;

		this.objLocation= NetworkUtil.registryLookup(registryLocation,name).address;
		this.name = name;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("[InvocationHandler] methodName:" + method.getName());
		if(method.getName().equals("getRMIName")){
			return this.name;
		}
		//In both the cases where we are trying to pass a remote object or remote object stub 
		//as an argument, we need to recreate the stub on the other side
		for(int i = 0; i < args.length; i++){
			if(RemoteObj.class.isAssignableFrom(args[i].getClass())){
				RemoteObj r = (RemoteObj) args[i];
				RemoteObjectRef obj = NetworkUtil.registryLookup(registryLocation, r.getRMIName());
				System.out.println("[InvocationHandler] works");
				args[i] = obj;
			}
			//Otherwise, we just use the copied object
		}
		
		RMIMessage msg = new RMIMessage(RMIMessage.RMIMessageType.INVOKE, 
										args, name, method.getName(), method.getParameterTypes());
		
		RMIMessage response = null;
		try {
			Socket client = new Socket(objLocation.getAddress(), objLocation.getPort());

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
				return response.getArguments()[0];
			case EXCEPTION:
				Exception e = (Exception)(response.getArguments()[0]);
				throw e;
			default://shouldn't be reached
		}
		
		//also shouldn't be reached
		return null;
	}

}
