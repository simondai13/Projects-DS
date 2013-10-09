package example;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import rmi_framework.*;

public class Main {

	public static void main(String[] args) throws Exception{
		
		//connect to registry
		RemoteObjectRegistry registry = new RemoteObjectRegistry(5444);
		
		InetSocketAddress regAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5444);
		
		//create new RMIHandler on this node

		RMIHandler rh = new RMIHandler(regAddress, 5445);
		InetSocketAddress handlerAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5445);
		
		//add local objects to each of those
		
		StringReverse sr = new StringReverse("helloworld");

		
		
		StringReverse_stub st;
				
		//add remote? objects?
		
		//try having the local RMIHandler node call a method
	}
}
