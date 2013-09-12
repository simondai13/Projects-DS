import java.util.*;
import java.net.*;
import java.io.*;


public class ProcessManagerSlave extends ProcessManager {

	private Socket master;
	
	//Constructor for a slave node ProcessManager
	//Stores a single instance of 
	public ProcessManagerSlave(int port_num, String masterAddress, int masterPort) throws UnknownHostException, IOException{
		
		super(port_num);
		master = new Socket(masterAddress,masterPort);
	}
	
	
	public String runningOn(MigratableProcess p){
		
		//Ask Master
		
		return null;
	}

	public void run()
	{
		 try{
			 server = new ServerSocket(port_num); 
		 } catch (IOException e) {
			 //We cannot run a process server on this machine if the port is problematic
			 System.out.println("Could not listen on port " +Integer.toString(port_num));
			 return;
		}
		 while(true) {
			 Socket client;
			 try{
				 client = server.accept();
				 InputStream in = client.getInputStream();
				 //Now we de-serialize the object
				 InputStream buffer= new BufferedInputStream(in);
				 ObjectInput obj = new ObjectInputStream(buffer);
				 MigratableProcess process = null;
				 process = (MigratableProcess) obj.readObject();
				 //Run the process on this machine
				 startProcess(process);
			 
			 } catch (ClassNotFoundException e) {
				 //Send response
			 } catch (IOException e) {
				 System.out.println("Connection to client failed.");
			 }
		 }
		 
	}
	
}
