package example;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import rmi_framework.NetworkUtil;
import rmi_framework.RMIHandler;

public class Main2 {

	public static void main(String[] args) throws Exception{
		

		InetSocketAddress regAddress =  new InetSocketAddress(InetAddress.getLocalHost(), 5444);
		//get stub for the StringReverse class run in the main class
		StringReverse reverser = (StringReverse)RMIHandler.getRemoteObject(regAddress, "stringReverse1");
		
		System.out.println(reverser.reverse("ABCDE", 4));

		RMIHandler rh = new RMIHandler(regAddress, 5446);
		InetSocketAddress handlerAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5446);
		Thread handlerThread = new Thread(rh);
		handlerThread.start();
		
		GlobalStringReverse gsr = new GlobalStringReverseImpl(4, "globalStringReverse1");
		rh.registerObject(gsr, GlobalStringReverse.class);
		
		System.out.println("DONE");
	}
}
