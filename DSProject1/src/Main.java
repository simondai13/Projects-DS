import java.io.IOException;


public class Main {

	public static void main(String args[]) throws InterruptedException, IOException{

		ProcessManagerServer server = new ProcessManagerServer(1257);
		Thread t1 = new Thread(server);
		t1.start();
		
		ProcessManagerClient client1 = new ProcessManagerClient(1255,"localhost",1257);
		Thread t2 = new Thread(client1);
		t2.start();
		
		ProcessManagerClient client2 = new ProcessManagerClient(1256,"localhost",1257);
		Thread t3 = new Thread(client2);
		t3.start();
		
		String[] s = {"100"};
		MigratableProcess mp = new TimeBomb(s);
		long mpID = client1.startProcess(mp);
		
		System.out.println("Running on " + client1.checkStatus(mpID));
		

		Thread.sleep(500);
		client1.migrateProcess("localhost",1256, mp);
		System.out.println("Running on " + client1.checkStatus(mp));
		Thread.sleep(500);
		client1.migrateProcess("localhost", 1255, mp);

		System.out.println("Running on " + client1.checkStatus(mp));

		Thread.sleep(500);
		
		String[] params = {"BadFileCopyIn.txt","BadFileCopyOut.txt"};
		MigratableProcess mp2 = new BadFileCopy(params);
		
		long id2 = client1.startProcess(mp2);
		
		Thread.sleep(400);
		
		System.out.println("Running on " + client1.checkStatus(id2));
		client1.migrateProcess("localhost", 1256, id2);
		System.out.println("Running on " + client1.checkStatus(id2));

		Thread.sleep(500);
		System.out.println("Status of process 2: " + client1.checkStatus(id2));
		Thread.sleep(500);
		System.out.println("Status of process 2: " + client1.checkStatus(id2));
		Thread.sleep(500);
		System.out.println("Status of process 2: " + client1.checkStatus(id2));
		Thread.sleep(500);
		System.out.println("Status of process 2: " + client1.checkStatus(id2));
		Thread.sleep(500);
		System.out.println("Status of process 2: " + client1.checkStatus(id2));
		System.out.println("DONE");
		
	}
	
}
