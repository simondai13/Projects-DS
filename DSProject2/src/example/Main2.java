package example;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import rmi_framework.RMIHandler;

public class Main2 {

	public static void main(String[] args) throws Exception{
		

		RMIHandler rh = new RMIHandler(Main.regAddress, 5445);
		InetSocketAddress handlerAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5445);
		Thread handlerThread = new Thread(rh);
		handlerThread.start();
		
		
	}
}
