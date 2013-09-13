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
	
	
	public void run()
	{
		 while(true) {
			 Socket client;
			 try{
				 client = server.accept();
				 InputStream in = client.getInputStream();
				 //HANDLE MASTER QUERIES FIRST
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


	@Override
	public List<String> runningProcesses() {

		List<String> toReturn = new ArrayList<String>();
		
		//call master version
		
		return null;
	}
	
}
