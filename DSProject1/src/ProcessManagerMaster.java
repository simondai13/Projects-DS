import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.InetAddress;
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

	public String runningOn(MigratableProcess p) {

		if(isRunningLocally(p)){
			try {
				return InetAddress.getLocalHost()+":"+server.getLocalPort();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		//ask every slave
		for(Socket s : slaves){
			
			//SEND MESSAGES
			
			//WAIT FOR REPLIES
		}
		return null;
	}

	@Override
	public void run() {

		 while(true) {
			 Socket client;
			 try{
				 client = server.accept();
				 InputStream in = client.getInputStream();
				 
				 //HANDLE DIFFERENT INPUTS (use headers?)
				 //IE, SLAVE QUERIES
				 
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
		try {
			for(MigratableProcess m : processes.keySet()){
				
				Thread t = processes.get(m);
				if(t.isAlive())
					toReturn.add(m.getClass().getName() + " "+InetAddress.getLocalHost()+":"+server.getLocalPort());
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return toReturn;
	}
}
