import java.util.*;
import java.net.*;
import java.io.*;


public abstract class ProcessManager implements Runnable {

	protected ServerSocket server;
	protected Map<MigratableProcess, Thread> processes;
	
	public static Object fileLock = new Object();

	public ProcessManager(){
		
		this(1255);
	}
	
	//Constructor for a slave node ProcessManager
	//Stores a single instance of 
	public ProcessManager(int port_num){

		 try{
			 server = new ServerSocket(port_num); 
		 } catch (IOException e) {
			 //We cannot run a process server on this machine if the port is problematic
			 System.out.println("Could not listen on port " +Integer.toString(port_num));
			 return;
		}
		processes = new HashMap<MigratableProcess, Thread>();
	}
	
	
	public void startProcess(MigratableProcess p) {
		//Make sure that this instance of a MigratableProcess is not
		//already running,  a new instance of a MigratableProcess is required for each
		//thread
		if(processes.containsKey(p)) {
			System.out.println("Attempting to start the same migratable process multiple times, create a new instance");
			return;
		}
		Thread processThread = new Thread(p);
		processThread.start();
		processes.put(p, processThread);
	}
	
	//Add functionality for suspend, query, etc
	public boolean isRunningLocally(MigratableProcess p){
		
		Thread pThread = processes.get(p);
		if(pThread != null){
			
			return pThread.isAlive();
		}
		
		return false;
	}
		
	public void migrateProcess(String newAddress, int port, MigratableProcess p){
		try{
			
			processes.remove(p);
			p.suspend();
			synchronized(ProcessManager.fileLock){
				Socket client = new Socket(newAddress,port);
				OutputStream out = client.getOutputStream();
				OutputStream buffer = new BufferedOutputStream(out);
				ObjectOutput output = new ObjectOutputStream(buffer);
				output.writeObject(p);
				output.close();
				client.close();
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public abstract List<String> runningProcesses();
	public abstract void run();
	
}
