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
	 
	//Map of node to currently executing tasks
	//Keeps an array for parallel job execution
	public Map<InetSocketAddress,List<Task>> jobs; 
	public Map<InetSocketAddress,Boolean> nodeStatus;
	//public Map<String,ArrayList<InetSocketAddress>> fileLocs;
	public Set<InetSocketAddress> activeNodes; //maintain a list of operable nodes
	public Map<InetSocketAddress,Integer> dfsPortToMRPort;
	private int portNum;
	private int numCores;
	private int numPartitions;
	private ServerSocket server;
	private Scheduler scheduler;
	private MasterDFS dfsMaster;
	volatile boolean isDone;
	volatile boolean mapCanceled;
	
	public Master(int port, int dfsPort,int replFactor,int numCores, int numPartions) throws IOException{
		server=new ServerSocket(port);
		portNum=port;
		this.numCores=numCores;
		this.numPartitions=numPartions;
		
		
		jobs=new TreeMap<InetSocketAddress,List<Task>>(new NodeCompare());
		nodeStatus= new TreeMap<InetSocketAddress,Boolean>(new NodeCompare());
		//fileLocs= new TreeMap<String,ArrayList<InetSocketAddress>>();
		activeNodes= new TreeSet<InetSocketAddress>(new NodeCompare());
		dfsPortToMRPort = null;
		
		//instantiate the scheduler only once all nodes have been initiated
		scheduler=null;
		
		//We are "done" until we are given a map reduce task
		isDone=true;
		
		//instantiate the dfs system
		dfsMaster=new MasterDFS(this,dfsPort,replFactor);
		Thread dfsThread=new Thread(dfsMaster);
		dfsThread.start();
	}

	private void startMapReduce(String mapReduce){
		mapCanceled=false;
		isDone=false;
		//Send in the full node array, not just the working ones, the master will
		//sort out failed nodes
		scheduler = new Scheduler(dfsMaster.fileNodes, dfsMaster.fileLocs,mapReduce,numCores, numPartitions);
		TreeMap<InetSocketAddress,List<Task>> tasks = scheduler.getInitialTasks();
		synchronized(jobs){
			for(Entry<InetSocketAddress,List<Task>> e :tasks.entrySet()){
				jobs.put(new InetSocketAddress(e.getKey().getAddress(),dfsPortToMRPort.get(e.getKey())),new ArrayList<Task>());
			}
		}
		for(Entry<InetSocketAddress,List<Task>> e :tasks.entrySet())
		{	
			for(Task t : e.getValue()){
				sendTask(t,new InetSocketAddress(e.getKey().getAddress(),dfsPortToMRPort.get(e.getKey())));
			}
		}
	}
	
	private class WorkerCheck extends TimerTask {
		
		@Override
		public void run() {
			synchronized(jobs) {
				for (Entry<InetSocketAddress,Boolean> entry : nodeStatus.entrySet())
				{
					if(!entry.getValue()  &&activeNodes.contains(entry.getKey()))
					{
						handleFailure(entry.getKey());
						System.out.println("Lost connection to: " + entry.getKey().getAddress()+ "attempting recovery");
					}
					entry.setValue(false);
				}
			}
			
		}
	}
	
	public void handleFailure(InetSocketAddress node){
		synchronized(jobs){
			List<Task> tasks= jobs.get(node);
			for(Task t: tasks){
				scheduler.addTask(t);
			}
			jobs.put(node, new ArrayList<Task>());
			activeNodes.remove(node);
		}
	}
	
	//This method is called after a task completes and the scheduler has no new tasks,
	//Check to make sure all running jobs are complete
	public boolean checkIsCompleted(){
		boolean done=true;
		synchronized(jobs){
			for(List<Task> tasks : jobs.values()){
				if(!tasks.isEmpty()){
					done=false;
				}
			}
		}
		if(done && scheduler.initialMapsComplete)
			return true;
		else{
			scheduler.initialMapsComplete=true;
			return false;
		}
	}
	
	//Process a nodes status update and reacts accordingly
	public void updateNodeStatus(StatusUpdate stat){
			switch(stat.type)
			{
			case FAILED: //Might be a reduce task that failed because of map result lost, get the next task 
				scheduler.addTask(stat.task);
				sendTask(scheduler.getNextTask(stat.node),stat.node);
				break;
			case TERMINATED:
				Task next= scheduler.getNextTask(stat.node);
				synchronized(jobs){
					List<Task> tasks= jobs.get(stat.node);
					int taskIndex=0;
					for(int i=0; i< tasks.size(); i++){
						if(tasks.get(i).PID ==stat.task.PID)
							taskIndex=i;
					}
					tasks.remove(taskIndex);
				}
				
				if(next!=null)
					sendTask(next,stat.node);
				else
					isDone = checkIsCompleted();
				break;
			default:
				break;
			}
			
	}
	
	private void terminateAll(){
		mapCanceled=true;
		//Send messages to all workers to kill all active tasks
		try{
			synchronized(jobs){
			for(Map.Entry<InetSocketAddress,List<Task>> e : jobs.entrySet()){
				List<Task> tasks = e.getValue();
				for(Task t: tasks){
					Socket client = new Socket(e.getKey().getAddress(),e.getKey().getPort());
					OutputStream out = client.getOutputStream();
					ObjectOutput objOut = new ObjectOutputStream(out);
					InputStream in = client.getInputStream();
					ObjectInput objIn = new ObjectInputStream(in);
					KillTask kt = new KillTask();
					kt.PID=t.PID;
					
					objOut.writeObject(kt);
					client.close();
				}
			}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//Clear the jobs
		synchronized(jobs){
			for(Map.Entry<InetSocketAddress, List<Task>> j : jobs.entrySet()){
				jobs.put(j.getKey(), new ArrayList<Task>());
			}
		}
		isDone = true;
	}
	
	
	public void sendTask(Task t, InetSocketAddress node){
		List<Task> tasks= jobs.get(node);
		tasks.add(t);
		
		try
		{
			System.out.println("Sending Task " + t.PID);
			if(dfsPortToMRPort==null){
				System.out.println("No node configuration");
			}
			Socket client = new Socket(node.getAddress(),node.getPort());
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
			InputStream in = client.getInputStream();
			ObjectInput objIn = new ObjectInputStream(in);
			
			objOut.writeObject(t);
			
			client.close();
				
		} catch (IOException e) {
			System.out.println("Error: Unable to connect to Worker");
			e.printStackTrace();
		}
		
	}
	
	//Check if a file node is active by checking its corresponding mapreduce status
	public boolean isActiveFileNode(InetSocketAddress a){
		synchronized (activeNodes)
		 {
			return activeNodes.contains(new InetSocketAddress(a.getAddress(),dfsPortToMRPort.get(a)));
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
						 if(!activeNodes.contains(stat.node)){
							 System.out.println("Recovered node at " +stat.node.getAddress());
							 activeNodes.add(stat.node);
							 nodeStatus.put(stat.node,true);
						 }
						 
						 switch(stat.type) {
						 	case HEARTBEAT: 
						 		nodeStatus.put(stat.node,true);
						 		break;
						 	default:
						 		if(!mapCanceled){
						 			updateNodeStatus(stat);
						 		}
						 }
					 }
				 } else if(MasterControlMsg.class.isAssignableFrom(obj.getClass()))
				 {
					 //Note that all these control messages return some result.
					 //This is to force the API to block until these tasks are actually complete
					 OutputStream out = client.getOutputStream();
					 ObjectOutput objOut = new ObjectOutputStream(out);
					 
					 MasterControlMsg msg = (MasterControlMsg) obj;
					 switch(msg.type) {
					 	case START:
					 		startMapReduce(msg.mapReduce);
					 		
							objOut.writeObject(new String("OK"));
					 		break;
					 	case TERMINATE:
					 		terminateAll();
							objOut.writeObject(new String("OK"));
					 		break;
					 	case QUERY:
					 		MapReduceState resp = new MapReduceState();
					 		synchronized(jobs){
						 		resp.activeJobs=jobs;
						 		resp.activeNodes=activeNodes;
						 		resp.fileLocs=dfsMaster.fileLocs;
						 		resp.isDone=isDone;
						 		
								objOut.writeObject(resp);
					 		}
							break;
					 	default:
					 		break;
						 
					 }
				 } else if(MasterConfiguration.class.isAssignableFrom(obj.getClass())){
					 System.out.println("Node configuration recieved");
					 MasterConfiguration cfg = (MasterConfiguration) obj;
					 dfsPortToMRPort= cfg.dfsToMRports;
					 for(Map.Entry<InetSocketAddress, Integer> e: dfsPortToMRPort.entrySet()){
						 jobs.put(new InetSocketAddress(e.getKey().getAddress(),e.getValue()),new ArrayList<Task>());
						 nodeStatus.put(new InetSocketAddress(e.getKey().getAddress(),e.getValue()),true);
						 activeNodes.add(new InetSocketAddress(e.getKey().getAddress(),e.getValue()));
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
		timer.scheduleAtFixedRate(new WorkerCheck(),10000,3000);
		
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
