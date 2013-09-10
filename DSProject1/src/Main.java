import java.net.InetAddress;
import java.net.UnknownHostException;


public class Main {

	public static void main(String args[]) throws UnknownHostException, InterruptedException{

		ProcessManager pm = new ProcessManager(1255);
		
		Thread t1 = new Thread(pm);
		t1.start();
		String[] s = {"100"};
/*
		MigratableProcess mp = pm.startProcess("TimeBomb", s);
		
		Thread.sleep(500);
		InetAddress adr = InetAddress.getLocalHost();
		System.out.println("sending migration request for TimeBomb");
		pm.migrateProcess(adr, mp);
	*/	
		String[] params = {"105","TimeBombOut.txt"};
		MigratableProcess mp2 = pm.startProcess("TimeBombFile", params);
		
		Thread.sleep(750);
		InetAddress adr = InetAddress.getLocalHost();
		pm.migrateProcess(adr, mp2);
		
		System.out.println("DONE");
	}
	
}
