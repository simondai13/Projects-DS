import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;





public class Master implements Runnable{
	 
	//private TreeMap<Long,StatusUpdate.Type> jobs;
	//Map of node to currently executing tasks
	//Keeps an array for parallel job execution
	private TreeMap<InetSocketAddress,Task[]> jobs; 
	private TreeMap<InetSocketAddress,Boolean> nodeStatus;
	private TreeMap<String,InetSocketAddress[]> fileLocs;
	private TreeSet<InetSocketAddress> activeNodes; //maintain a list of operable nodes
	private long heartRate;
	private long delay;
	private int portNum;
	private ServerSocket server;
	private Scheduler scheduler;
	
	//heartbeatPeriod <<heartbeat on compute node
	public Master(long heartbeatPeriod,long delay, int port) throws IOException{
		server=new ServerSocket(port);
		portNum=port;
		
		heartRate=2*heartbeatPeriod;
		this.delay=delay;
		
		jobs=new TreeMap<InetSocketAddress,Task[]>();
		nodeStatus= new TreeMap<InetSocketAddress,Boolean>();
		fileLocs= new TreeMap<String,InetSocketAddress[]>();
		activeNodes= new TreeSet<InetSocketAddress>();
		
		//instantiate the scheduler only once all nodes have been initiated
		scheduler=null;
	}
	
	private void addNode(InetSocketAddress node){
		synchronized (activeNodes)
		 {
			activeNodes.add(node);
		
		 }
	}
	
	private class WorkerCheck extends TimerTask {
		//Checks at a fixed interval if the running 
		@Override
		public void run() {
			synchronized(jobs) {
				synchronized (activeNodes)
				 {
					for (Entry<InetSocketAddress,Boolean> entry : nodeStatus.entrySet())
					{
						if(!entry.getValue()  &&activeNodes.contains(entry.getValue()))
						{
							handleFailure(entry.getKey());
							System.out.println("Lost connection to: " + entry.getKey().getAddress()+ "attempting recovery");
						}
						entry.setValue(false);
					}
				 }
			}
		}
	}
	
	public void handleFailure(InetSocketAddress node){
		 
	}
	
	//Process a nodes status update and reacts accordingly
	public void updateNodeStatus(StatusUpdate stat){
			switch(stat.type)
			{
			case FAILED: //simply resend the task and hope it works 
				sendTask(stat.task,stat.node);
				break;
			case TERMINATED:
				sendTask(scheduler.getNextTask(stat.node),stat.node);
				break;
			default:
				break;
			}
			
	}
	
	public void sendTask(Task t, InetSocketAddress node){
		try
		{
			Socket client = new Socket(node.getAddress(),node.getPort());
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
			
			objOut.writeObject(t);

			InputStream in = client.getInputStream();
			ObjectInput objIn = new ObjectInputStream(in);
			
			//Read a dummy reply 
			objIn.readObject();
			

			client.close();
				
		} catch (IOException e) {
			System.out.println("Error: Unable to connect to Worker");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//TODO add some abort function
	public void abort(){}
	
	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		
		//Handle the life cycle of a remote object query, and then let this thread die
		@Override
		public void run() {
			try{ 
				 InputStream in = client.getInputStream();
				 ObjectInput objIn = new ObjectInputStream(in);
				 Object obj = objIn.readObject();
				 
				 //Make sure this message is packed properly
				 if(StatusUpdate.class.isAssignableFrom(obj.getClass()))
				 {
			
					 StatusUpdate stat = (StatusUpdate) obj;
					 synchronized (jobs)
					 {
						 switch(stat.type) {
						 	case HEARTBEAT: 
						 		nodeStatus.put(stat.node,true);
						 		break;
						 	default:
						 		updateNodeStatus(stat);
						 }
					 }
				 }
				 else if(FileLocationRequest.class.isAssignableFrom(obj.getClass()))
				 {
					 OutputStream out = client.getOutputStream();
					 ObjectOutput objOut = new ObjectOutputStream(out);
					 
					 //Send the first valid node address with this data
					 //If the node fails when the worker tries to contact it, the worker
					 //then has to send the master another request
					 FileLocationRequest req = (FileLocationRequest) obj;
					 InetSocketAddress[] locs = fileLocs.get(req.filename);
					 
					 boolean locFound=false;
					 for(int i=0; i<=locs.length; i++){
						 synchronized (activeNodes)
						 {
							 if(activeNodes.contains(locs[i])){
								 objOut.writeObject(locs[i]);
								 locFound=true;
							 }
						 }
					 }
					 
					 if(!locFound){
						 System.out.println("CRITICAL ERROR: ALL INSTANCES OF " + req.filename + "MISSING");
						 abort();
					 }
				}
				 
			 } catch (ClassNotFoundException e) {
				 System.out.println("Invalid Process Request");
			 }  catch (IOException e) {
				 System.out.println("Error receiving client message.");
			 }
		}
	}
		
	
	@Override
	//Simply accept connections and spawn ConnectionHandles as needed for this client
	public void run() {
		//Initialize the heartbeat monitor
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new WorkerCheck(),delay,heartRate);
		
		while(true) {
			try {
				Socket client = server.accept();
				
				//Generate a connection handle and run it in a 
				//separate thread
				ConnectionHandle ch = new ConnectionHandle(client);
				Thread t = new Thread(ch);
				t.start();
				
			} catch (IOException e) {
				
				System.out.println("CONNECTION FAILURE");
				// Just chalk it up to a failed connection and keep
				// running
			}
			 
				 
		}
				 
	}
}
