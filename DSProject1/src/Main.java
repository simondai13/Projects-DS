import java.net.InetAddress;
import java.net.UnknownHostException;


public class Main {

	public static void main(String args[]) throws UnknownHostException, InterruptedException{

		ProcessManager pm = new ProcessManager(1255);
		
		Thread t1 = new Thread(pm);
		t1.start();
		
		String adr = "localhost";//"71.206.238.246";
		
		/*
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
