package example;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import rmi_framework.*;

public class Main {
	
	public static void main(String[] args) throws Exception{
		
		//connect to registry
		RemoteObjectRegistry registry = new RemoteObjectRegistry(5444);
		Thread registryThread = new Thread(registry);
		registryThread.start();
		
		InetSocketAddress regAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5444);
		
		//create new RMIHandler on this node

		RMIHandler rh = new RMIHandler(regAddress, 5445);
		InetSocketAddress handlerAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5445);
		Thread handlerThread = new Thread(rh);
		handlerThread.start();
		
		//add local objects to each of those
		StringReverseImpl sr = new StringReverseImpl("helloworld","stringReverse1");
		rh.registerObject(sr, StringReverse.class);
		
		System.out.println("Run Main2, then press enter");
		System.in.read();
		

		GlobalStringReverse globalReverser = (GlobalStringReverse)RMIHandler.getRemoteObject(regAddress, "globalStringReverse1");
		
		List<String> l = new ArrayList<String>();
		l.add("This ");
		l.add("IS A ");
		l.add("Testing");
		l.add("Mechanism");
		l.add("1234567");
		
		System.out.println(globalReverser.globalReverse(l, sr));

		l.set(0, "TH");
		System.out.println(globalReverser.globalReverse(l, sr));
		System.out.println("DONE");
	}
}
