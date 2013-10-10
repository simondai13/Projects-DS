package example;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import rmi_framework.NetworkUtil;
import rmi_framework.RMIHandler;
import rmi_framework.RemoteObjectRegistry;

public class MainStub {

	public static void main(String[] args) throws Exception
	{
		RemoteObjectRegistry registry = new RemoteObjectRegistry(5444);
		Thread registryThread = new Thread(registry);
		registryThread.start();
		
		
		//Create an RMI handler
		InetSocketAddress regAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5444);
		RMIHandler rh = new RMIHandler(regAddress, 5445);
		InetSocketAddress handler1Address = new InetSocketAddress(InetAddress.getLocalHost(), 5445);
		Thread handlerThread = new Thread(rh);
		handlerThread.start();
		//Create a database and add stuff
		Database db1= new TreeDatabase("Database1");
		db1.setName(1, "John");
		db1.setName(2, "Alex");
		db1.setName(3, "Kevin");
		db1.setName(4, "Ma-the-vaa");
		//Add this to our first registry (port 5445)
		rh.registerObject(db1, Database.class);
	
		
		RMIHandler rh2 = new RMIHandler(regAddress, 5446);
		InetSocketAddress handler2Address = new InetSocketAddress(InetAddress.getLocalHost(), 5446);
		Thread handler2Thread = new Thread(rh2);
		handler2Thread.start();
		//Create a second database and add stuff
		Database db2 = new TreeDatabase("Database2");
		db2.setName(5, "Jake");
		db2.setName(6, "Josh");
		db2.setName(7, "Chris");
		rh.registerObject(db2, Database.class);

		//Get some stubs to our databases
		Database d1Ref = (Database) RMIHandler.getRemoteObject(regAddress, "Database1");
		Database d2Ref = (Database) RMIHandler.getRemoteObject(regAddress, "Database2");

		//The second database should now have all the items in our first database
		d1Ref.copyTo(d2Ref);
		db2.printContents();
		
		//Testing returning Remote objects
		Database d2Ref2 = d2Ref.getDatabase();
		System.out.println(" ");
		d2Ref2.printContents();
		System.out.println(" ");
		
		//Try RMI with some arbitrary exception
		try {
			d2Ref2.exceptionTest();
		} catch (NumberFormatException e1) {
			System.out.println("Exception properly passed!");
		}
		
	}

}
