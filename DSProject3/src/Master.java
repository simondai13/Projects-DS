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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;





public class Master implements Runnable{
	 
	//private TreeMap<Long,StatusUpdate.Type> jobs;
	//Map of node to currently executing tasks
	//Keeps an array for parallel job execution
	public Map<InetSocketAddress,Task[]> jobs; 
	public Map<InetSocketAddress,Boolean> nodeStatus;
	public Map<String,ArrayList<InetSocketAddress>> fileLocs;
	public Set<InetSocketAddress> activeNodes; //maintain a list of operable nodes
	public Map<InetSocketAddress,Integer> dfsPortToMRPort;
	public List<InetSocketAddress> activeFileNodes;
	private long heartRate;
	private long delay;
	private int portNum;
	private ServerSocket server;
	private Scheduler scheduler;
	private MasterDFS dfsMaster;
	
	//heartbeatPeriod <<heartbeat on compute node
	public Master(long heartbeatPeriod,long delay, int port, int dfsPort,int replFactor) throws IOException{
		server=new ServerSocket(port);
		portNum=port;
		
		heartRate=2*heartbeatPeriod;
		this.delay=delay;
		
		jobs=new TreeMap<InetSocketAddress,Task[]>();
		nodeStatus= new TreeMap<InetSocketAddress,Boolean>();
		fileLocs= new TreeMap<String,ArrayList<InetSocketAddress>>();
		activeNodes= new TreeSet<InetSocketAddress>();
		dfsPortToMRPort = null;
		
		//instantiate the scheduler only once all nodes have been initiated
		scheduler=null;
		
		//instantiate the dfs system
		dfsMaster=new MasterDFS(this,dfsPort,replFactor);
		Thread dfsThread=new Thread(dfsMaster);
		dfsThread.start();
	}

	private void startMapReduce(String mapReduce){
		//Send in the full node array, not just the working ones, the master will
		//sort out failed nodes
		scheduler = new Scheduler(dfsMaster.fileNodes, dfsMaster.fileLocs,mapReduce);
		TreeMap<InetSocketAddress,Task> tasks = scheduler.getInitialTasks();
		System.out.println("Starting Map Reduce Task");
		for(Entry<InetSocketAddress,Task> e :tasks.entrySet())
		{
			sendTask(e.getValue(),new InetSocketAddress(e.getKey().getAddress(),dfsPortToMRPort.get(e.getKey())));
		}
	}
	
	private class WorkerCheck extends TimerTask {
		
		@Override
		public void run() {
			/*synchronized(jobs) {
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
			*/
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
			System.out.println("Sending Task" + t.PID);
			if(dfsPortToMRPort==null){
				System.out.println("No node configuration");
			}
			Socket client = new Socket(node.getAddress(),node.getPort());
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
			
			objOut.writeObject(t);
			client.close();
				
		} catch (IOException e) {
			System.out.println("Error: Unable to connect to Worker");
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
					 System.out.println("Got status update from");
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
				 } else if(MasterControlMsg.class.isAssignableFrom(obj.getClass()))
				 {
					 MasterControlMsg msg = (MasterControlMsg) obj;
					 switch(msg.type) {
					 	case START:
					 		System.out.println(msg.mapReduce + "AAAAAAAAAAAAAAAAAAAAA");
					 		startMapReduce(msg.mapReduce);
					 		break;
					 	default:
					 		break;
						 
					 }
				 } else if(MasterConfiguration.class.isAssignableFrom(obj.getClass())){
					 System.out.println("Node configuration recieved");
					 MasterConfiguration cfg = (MasterConfiguration) obj;
					 dfsPortToMRPort= cfg.dfsToMRports;
					 if(dfsPortToMRPort==null)
						 System.out.println("PBOB");
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
