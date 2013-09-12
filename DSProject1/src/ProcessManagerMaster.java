import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;


public class ProcessManagerMaster extends ProcessManager {

	private List<Socket> slaves;
	
	public ProcessManagerMaster(int port_num) throws UnknownHostException, IOException{
		
		super(port_num);
		slaves = new ArrayList<Socket>();
	}

	@Override
	public String runningOn(MigratableProcess p) {

		if(isRunningLocally(p))
			return server.getInetAddress()+"";
		//ask every slave
		for(Socket s : slaves){
			
			//SEND MESSAGES
			
			//WAIT FOR REPLIES
		}
		return null;
	}

	@Override
	public void run() {

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
				 
				 //HANDLE DIFFERENT INPUTS (use headers?)
				 
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
