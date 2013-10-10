package rmi_framework;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

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

    RemoteObj localise(InetSocketAddress registryLocation)
    {
    	InvocationHandler handler = new RMIStubInvocationHandler(registryLocation,name);
    	return (RemoteObj) Proxy.newProxyInstance(interfaceType.getClassLoader(),
                					new Class[] { interfaceType },
                					handler);
    }
}