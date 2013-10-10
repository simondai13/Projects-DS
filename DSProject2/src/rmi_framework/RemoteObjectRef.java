package rmi_framework;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

/*Class that contains all the information necessary for 
 * referencing a specific object over a network.  This includes the type,
 * socket address, and name.  
 */
public class RemoteObjectRef implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	InetSocketAddress address;
    String name;
    Class<?> interfaceType;

    public RemoteObjectRef(InetSocketAddress address, String name, Class<?> interfaceType) 
    {
    	this.address=address;
    	this.name=name;
    	this.interfaceType = interfaceType;
    }

    //Creates a runtime stub for the current RemoteObjectReference.  This stub implements the interface
    //type, see documentation for Proxy (java API).
    RemoteObj localise(InetSocketAddress registryLocation)
    {
    	InvocationHandler handler = new RMIStubInvocationHandler(registryLocation,name);
    	return (RemoteObj) Proxy.newProxyInstance(interfaceType.getClassLoader(),
                					new Class[] { interfaceType },
                					handler);
    }
}