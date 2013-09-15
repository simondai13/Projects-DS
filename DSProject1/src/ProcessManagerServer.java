import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.TreeMap;


public class ProcessManagerServer implements Runnable {
	private ServerSocket server;
	//As it is currently set up, terminatedProcesses is unbounded and thus
	//for a process manager that handles many processes over its life, it might be best to bound this
	private Vector<Long> terminatedProcesses;
	
	private TreeMap<Long, NodeAddr> processLocations;
	
	public ProcessManagerServer(int portNum) throws IOException {
		terminatedProcesses = new Vector<Long>();
		processLocations = new TreeMap<Long, NodeAddr>();
		server=new ServerSocket(portNum);
	}
	
	
	//Run a new instance of this class with each connection to 
	//Allow concurrent requests to be serviced and redirected
	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		@Override
		public void run() {
			try{
				 OutputStream out = client.getOutputStream();
				 ObjectOutput objOut = new ObjectOutputStream(out);
				 
				 InputStream in = client.getInputStream();
				 ObjectInput objIn = new ObjectInputStream(in);
				 
				 ProcessRequest pr = (ProcessRequest) objIn.readObject();
				 
				 synchronized(terminatedProcesses){
					 if(pr.req == RequestType.STATUS)
					 {
						 if(terminatedProcesses.contains(pr.guid))
						 {
							 pr.resp = ResponseType.TERMINATED;
						 }
						 else
						 {
							 //Pass this response to the host that is running it
						 NodeAddr addr = processLocations.get(pr.guid);
						 pr = forwardRequest(pr, addr.address , addr.port);
						 
						 //Cache the terminated process if necessary 
							 if(pr.resp == ResponseType.TERMINATED) {
								 processLocations.remove(pr.guid);
								 terminatedProcesses.add(pr.guid);
							 }
						 } 
					 } else if (pr.req == RequestType.MIGRATE) {
						 if(terminatedProcesses.contains(pr.guid))
						 {
							 pr.resp = ResponseType.TERMINATED;
						 } else {
							 NodeAddr addr = processLocations.get(pr.guid);
							 pr = forwardRequest(pr, addr.address , addr.port);
							 if(pr.resp == ResponseType.MIGRATE_OK){
								 processLocations.put(pr.guid, pr.destination);
							 }
						 }
					 } else if (pr.req == RequestType.LAUNCH) {
						 processLocations.put(pr.guid,pr.destination);
						 //We simply re-send the original request as a response, no response data is needed
					 }
				 }
				 
				 //Send the response to the original requester
				objOut.writeObject(pr);
				 
			 } catch (ClassNotFoundException e) {
				 System.out.println("Invalid Process Request");
			 }  catch (IOException e) {
				 System.out.println("Error receiving client message.");
					 e.printStackTrace();
				 }
			}
		//Forwards the request pr to the address addr with port number port, and returns 
		//the request result  Note, unfortunately this whole socket communication has to
		//be done within mutex control to ensure that a process is not moved while another
		//is trying to access it.
		private ProcessRequest forwardRequest(ProcessRequest pr, String addr, int port)
		{
			ProcessRequest resp = pr;
			try
			{
				Socket client = new Socket(addr,port);
				
				OutputStream out = client.getOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(out);
				
				InputStream in = client.getInputStream();
				ObjectInput objIn = new ObjectInputStream(in);
			
				//Forward the request
				objOut.writeObject(pr);

				//Blocking call, read the object response
				resp = (ProcessRequest) objIn.readObject();
				client.close();
				
			} catch (IOException e) {
				resp.resp = ResponseType.LOST;
			} catch (ClassNotFoundException e) {
				resp.resp = ResponseType.LOST;
			}
			
			return resp;		
		}
	}
	
	@Override
	public void run() {
		while(true) {
			
			try {
				
				Socket client = server.accept();
				//Generate a connection handle and run it in a 
				//separate thread
				ConnectionHandle ch = new ConnectionHandle(client);
				Thread t = new Thread(ch);
				t.start();
				
			} catch (IOException e) {
				// Just chalk it up to a failed connection and keep
				//running
			}
			 
				 
		}
				 
	}
			 

}
