package example;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import rmi_framework.*;

public class Main {

	public static InetSocketAddress regAddress;
	
	public static void main(String[] args) throws Exception{
		
		//connect to registry
		RemoteObjectRegistry registry = new RemoteObjectRegistry(5444);
		Thread registryThread = new Thread(registry);
		registryThread.start();
		
		regAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5444);
		
		//create new RMIHandler on this node

		RMIHandler rh = new RMIHandler(regAddress, 5445);
		InetSocketAddress handlerAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5445);
		Thread handlerThread = new Thread(rh);
		handlerThread.start();
		
		//add local objects to each of those
		StringReverse sr = new StringReverse("helloworld");
		rh.registerObject(sr, 0);
		
		//add remote? objects?
		
		//try having the local RMIHandler node call a method
		
		System.out.println("done");
	}
}
