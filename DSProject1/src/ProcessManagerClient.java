import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

/*
 * An instance of this class is equivalent to a node in the distributed system.  It runs MigratableProcesses
 * in addition to allowing the user to migrate these processes to another ProcessManagerClient on the network.  
 * Furthermore, the user can query running processes by id or object reference across the network.
 */
public class ProcessManagerClient implements Runnable {
	
	//The host and port on which the ProcessManagerServer is running
	private InetAddress pmServer;
	private int pmPort;
	
	//The local "server" for this client that accepts incoming communication
	private ServerSocket server;
	
	//Keep track of processes running locally to this machine to service query's regarding these processes
	private HashMap<Long,Thread> processes;
	private HashMap<MigratableProcess,Long> processIDs;
	
	//A single lock to synchronize file operations
	public static Object fileLock = new Object();
	
	//Begin a node in the network on this machine listening on port_num, connected to the ProcessManagerServer 
	//located at ip address pmServer listening on port pmPort
	public ProcessManagerClient(int port_num, InetAddress pmServer, int pmPort) throws IOException{

		this.pmServer = pmServer;
		this.pmPort = pmPort;
		
		//De-mystify any local host references
		if(pmServer.getHostName().equals("localhost"))
			this.pmServer = InetAddress.getLocalHost();
		
		server = new ServerSocket(port_num); 
		processes = new HashMap<Long,Thread>();
		processIDs = new HashMap<MigratableProcess,Long>();
	}
	
	//Start a migratableProcess on this machine that is synchronized with the process server
	public long startProcess(MigratableProcess p) {
		//Make sure that this instance of a MigratableProcess is not
		//already running,  a new instance of a MigratableProcess is required for each
		//thread
		if(processIDs.get(p) != null) {
			System.out.println("Attempting to start the same migratable process multiple times, create a new instance");
			return -1;
		}
		
		Thread processThread = new Thread(p);
		processThread.start();
		long id = newGuid();
		processes.put(id,processThread);
		processIDs.put(p,id);
		
		//Tell the process server about this process
		ProcessRequest pr = new ProcessRequest();
		pr.guid=id;
		pr.destination = new NodeAddr(server.getInetAddress(),server.getLocalPort());
		pr.req = RequestType.LAUNCH;
		sendProcessRequest(pr,pmServer,pmPort);
		return id;
	}
	
	//Resume MigratableProcess p on this machine
	private void resumeProcess(MigratableProcess p, long id) {
		
		Thread processThread = new Thread(p);
		processThread.start();
		processes.put(id,processThread);
		processIDs.put(p,id);
	}
	
	//Checks the status of a running process and returns the address
	//string of the host or null if the process is lost or terminated
	public String checkStatus(MigratableProcess p) {
		long id = processIDs.get(p);
		return checkStatus(id);
	}
	
	//Checks status of a running process and return a string indicating its status, either running, terminated,
	//or lost
	public String checkStatus(long id){

		ProcessRequest pr = new ProcessRequest();
		pr.guid=id;
		pr.req = RequestType.STATUS;
		ProcessRequest resp = sendProcessRequest(pr,pmServer,pmPort);
		if(resp.resp == ResponseType.RUNNING)
			return "Running on: " + resp.destination.address +":" + resp.destination.port;
		else if(resp.resp == ResponseType.TERMINATED)
			return "Terminated";
		else if (resp.resp == ResponseType.LOST)
			return "Lost";
		return null;
	}
	
	//migrates a process given a reference to the instance.  Note that p could be running on 
	//another machine
	public void migrateProcess(InetAddress newAddress, int port, MigratableProcess p){
		long id =processIDs.get(p);
		migrateProcess(newAddress, port ,id);
	}
	
	//Migrates a process with GUID id onto the machine with ip adress newAdress using port port.  Note that
	//need only reference a running process, not necessarily one that is on this machine
	public void migrateProcess(InetAddress newAddress, int port, long id) 
	{
		//Make sure no confusing "localhost" destinations are sent
		if(newAddress.getHostName().equals("localhost")){
			try{
				newAddress = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				return;
			}
		}
			
		
		ProcessRequest pr = new ProcessRequest();
		pr.guid=id;
		pr.destination = new NodeAddr(newAddress,port);
		pr.req = RequestType.MIGRATE;
		
		ProcessRequest resp = sendProcessRequest(pr,pmServer,pmPort);
		if(resp == null){
			System.out.println("Error connecting to process server");
		}
		else if(resp.resp==ResponseType.MIGRATE_FAILED)
		{
			System.out.println("Migration failed");
		}
		else if(resp.resp==ResponseType.LOST)
		{
			System.out.println("Migration failed: Failed to contact process host");
		}
		else if(resp.resp==ResponseType.TERMINATED)
		{
			System.out.println("Migration failed: process has already terminated");
		}
		else
		{
			System.out.println("Migration to: \"" + newAddress + ":" + port + "\" successful");
		}
	}
	
	//Attempts to migrate a process p that is running on this machine to newAddress using port port.  If the 
	//migration fails, the process will be resumed on this machine
	private boolean migrateLocalProcess(InetAddress newAddress, int port, MigratableProcess p)
	{
		try{
			p.suspend();
			synchronized(ProcessManagerClient.fileLock){
				Socket client = new Socket(newAddress,port);
				OutputStream out = client.getOutputStream();
				ObjectOutput output = new ObjectOutputStream(out);
				long tag =processIDs.get(p);
				TaggedMP tagP = new TaggedMP(p,tag);
				output.writeObject(tagP);
				processes.remove(tag);
			}
		}catch (IOException e) {
			//Migrating failed
			resumeProcess(p,processIDs.get(p)); //Resume process on this machine
			System.out.println("Attempt to Migrate Process failed");
			return false;
		}
		
		return true;
	}
	
	
	//Parse a ProcesRequest from the ProcessManagerServer, and place the reply message in a corresponding Process
	//Request to be sent back to the server.  
	private ProcessRequest parseRequest(ProcessRequest r)
	{
		if(r.req == RequestType.MIGRATE)
		{
			MigratableProcess p = getMP(r.guid);
			if(p==null)
			{
				r.resp=ResponseType.MIGRATE_FAILED;
				System.out.println("Unable to resolve process id");
				return r;
			} 
			Thread t = processes.get(r.guid);
			if(t == null || !t.isAlive()){
				r.resp=ResponseType.TERMINATED;
				System.out.println("Failed attempt to migrate terminated process");
			}else{
				
				if(migrateLocalProcess(r.destination.address, r.destination.port, p)) {
					r.resp= ResponseType.MIGRATE_OK;
				}
				else {
					r.resp= ResponseType.MIGRATE_FAILED;
				}
					
			}
		}
		if(r.req == RequestType.STATUS)
		{
			MigratableProcess p = getMP(r.guid);
			if(p==null)
			{
				r.resp=ResponseType.LOST;
				System.out.println("Unable to resolve process id");
				return r;
			} 
			Thread t = processes.get(r.guid);
			if(t == null || !t.isAlive()){
				r.resp=ResponseType.TERMINATED;
			}else{
				r.resp=ResponseType.RUNNING;
				InetAddress thisHost=null;
				try{
					thisHost = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					thisHost=null;
				}
				r.destination = new NodeAddr(thisHost, server.getLocalPort());
			} 
		}
		
		return r;
	}
	
	//Listen for connections made from the process server or other nodes.  The process server will send this node
	//process requests, and other nodes will simply ship MigratableProcesses to be run on this machine
	@Override
	public void run() {

		 while(true) {
			 Socket client;
			 try{
				client = server.accept();
				
				OutputStream out = client.getOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(out);
				 
				InputStream in = client.getInputStream();
				ObjectInput objIn = new ObjectInputStream(in);
				
				Object inputObject = objIn.readObject();
				
				if(ProcessRequest.class.isAssignableFrom(inputObject.getClass())){
					//We have received a request from the process server to do something
					ProcessRequest req = (ProcessRequest) inputObject;
					ProcessRequest response = parseRequest(req);
					objOut.writeObject(response);
				}else if (TaggedMP.class.isAssignableFrom(inputObject.getClass())){
					 TaggedMP process = (TaggedMP) inputObject;
					 //Run the process on this machine
					 resumeProcess(process.mp,process.id);
					 
					 //When receiving Migratible process, we opt to close the stream from the
					 //receiver to ensure that the entire object is read before the stream is closed
					 client.close();
				}
				
			 } catch (ClassNotFoundException e) {
				 System.out.println("Corrupted data recieved.");
			 } catch (IOException e) {
				 System.out.println("Connection to client failed.");
			 }
		 }
		 
	}
	
	//Sends a process request to the server and returns the response
	private ProcessRequest sendProcessRequest(ProcessRequest pr, InetAddress prServer, int port)
	{
		ProcessRequest resp = null;
		try
		{
			Socket client = new Socket(prServer,port);
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
		
			//Forward the request
			objOut.writeObject(pr);

			InputStream in = client.getInputStream();
			ObjectInput objIn = new ObjectInputStream(in);
			
			resp = (ProcessRequest) objIn.readObject();
			client.close();
			
		} catch (IOException e) {
			System.out.println("Error: Unable to connect to ProcessManager Server");
		} catch (ClassNotFoundException e) {
			System.out.println("Error: Invalid reponse from ProcessManager Server");
		}
		return resp;
	}
	
	
	//Create a new random 128 bit id for this process.  The probability of overlap is miniscule 
	private long newGuid()
	{
		Random rand = new Random();
		
		long guid=rand.nextLong();
		
		return guid;
	}
	
	//The mapping of processes to id's is bijective, this is just the inverse operation of a get operation on the 
	//map
	private MigratableProcess getMP(long id)
	{
		for (Entry<MigratableProcess, Long> e : processIDs.entrySet())
		{
			if(e.getValue() == id)
				return e.getKey();
		}
		return null;
	}
	

}
