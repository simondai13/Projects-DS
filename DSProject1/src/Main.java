import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


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
		client1.startProcess(mp);
		
		System.out.println("sending migration request for TimeBomb");
		client1.migrateProcess("localhost",1256, mp);
		
		client1.checkStatus(mp);
		Thread.sleep(1000*15);
		client1.checkStatus(mp);
		
		/*
		ProcessManager pm = new ProcessManagerMaster(1255);
		
		Thread t1 = new Thread(pm);
		t1.start();

		ProcessManager pm1 = new ProcessManagerMaster(1256);
		
		Thread t2 = new Thread(pm1);
		t2.start();
		
		String adr = "localhost";

		String[] s = {"100"};
		System.out.println("Starting process TimeBomb");
		
		MigratableProcess mp = new TimeBomb(s);
		pm.startProcess(mp);
		
		System.out.println("sending migration request for TimeBomb");
		
		pm.migrateProcess(adr,1256, mp);

		Thread.sleep(500);
		System.out.println(pm1.runningProcesses());
		/*
		
		//"71.206.238.246";
		
		
		String[] s = {"100"};
		System.out.println("Starting process TimeBomb");
		MigratableProcess mp = pm.startProcess("TimeBomb", s);
		
		Thread.sleep(500);
		System.out.println("sending migration request for TimeBomb");
		
		pm.migrateProcess(adr, mp);
		
		
		String[] params = {"105","TimeBombOut.txt"};
		System.out.println("Starting process TimeBombFile");
		MigratableProcess mp2 = pm.startProcess("TimeBombFile", params);
		
		Thread.sleep(750);
		System.out.println("sending migration request for TimeBombFile");
		pm.migrateProcess(adr, mp2);
		
		String[] params2 = {"BadFileCopyIn.txt", "BadFileCopyOut.txt"};
		BadFileCopy bfc = new BadFileCopy(params2);
		System.out.println("Starting process BadFileCopy");
		pm.startProcess(bfc);
		
		Thread.sleep(300);
		System.out.println("sending migration request for BadFileCopy");
		pm.migrateProcess(adr, 1255, bfc);
		
		
		
		System.out.println("DONE");*/
	}
	
}
