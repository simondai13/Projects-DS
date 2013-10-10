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

/*This class represents an abstracted stub.  It contains a single method, invoke,
 * that will pass the invocation as an RMIMessage to the actual client
 */
public class RMIStubInvocationHandler implements InvocationHandler {

	private InetSocketAddress registryLocation;
	private InetSocketAddress objLocation;
	private String name;
	
	public RMIStubInvocationHandler(InetSocketAddress registryLocation, String name){
		this.registryLocation=registryLocation;

		this.objLocation= NetworkUtil.registryLookup(registryLocation,name).address;
		this.name = name;
	}
	
	/*This method is called whenever a interface that extends RemoteObject calls a method (on a stub).
	*the details of the invocation are packaged into an RMIMessage and sent to the node that
	*contains the actual instance, and invokes the method there.  The results are then recieved, and
	*from the users perspective a local function has just been invoked.  Note this is also capable of 
	*passing and throwing errors from the actual instance
	*/
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//We do not need RMI to get the name, this prevents an infinite loop
		if(method.getName().equals("getRMIName")){
			return this.name;
		}
		//In both the cases where we are trying to pass a remote object or remote object stub 
		//as an argument, we need to recreate the stub on the other side
		for(int i = 0; i < args.length; i++){
			if(RemoteObj.class.isAssignableFrom(args[i].getClass())){
				RemoteObj r = (RemoteObj) args[i];
				RemoteObjectRef obj = NetworkUtil.registryLookup(registryLocation, r.getRMIName());
				args[i] = obj;
			}
			//Otherwise, we just use the copied object
		}
		
		RMIMessage msg = new RMIMessage(RMIMessage.RMIMessageType.INVOKE, 
										args, name, method.getName(), method.getParameterTypes());
		
		//Send the RMI message to the given location
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
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
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
