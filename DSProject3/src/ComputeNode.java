import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;


public class ComputeNode implements Runnable{

	
	private ServerSocket server;
	private InetSocketAddress master;
	private DFSNode dfs;
	private int portnum;
	private int numCores;
	private int numPartitions;
	//Simple data structure that does not need to be locked,
	//so it can be quickly checked each iteration of the map/reduce task
	volatile boolean[] killTask; 
	Map<Integer,Integer> pidToCore;
	
	public ComputeNode(int portnum, InetSocketAddress master,DFSNode dfs, int numCores, int numParts) throws IOException{
		this.portnum=portnum;
		this.master=master;
		this.numCores=numCores;
		server = new ServerSocket(portnum);
		this.dfs=dfs;
		killTask=new boolean[numCores];
		pidToCore=new TreeMap<Integer,Integer>();
		numPartitions=numParts;
	}
	
	private class Heartbeat extends TimerTask {
		
		@Override
		public void run() {
			try {
				Socket client = new Socket(master.getAddress(),master.getPort());
				OutputStream out = client.getOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(out);
				objOut.writeObject(new StatusUpdate(null, StatusUpdate.Type.HEARTBEAT, new InetSocketAddress(InetAddress.getLocalHost(),portnum)));
				
				client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		@Override
		public void run() {
			try{ 
				System.out.println("WORKS2");
				 OutputStream out = client.getOutputStream();
				 ObjectOutput objOut = new ObjectOutputStream(out);
				 
				 InputStream in = client.getInputStream();
				 ObjectInput objIn = new ObjectInputStream(in);
				 Object obj = objIn.readObject();
				 
				 //Make sure this message is packed properly
				 if(Task.class.isAssignableFrom(obj.getClass()))
				 {
					 Task t = (Task) obj;
					 
					 startNewTask(t);	
				 }
				 else if(KillTask.class.isAssignableFrom(obj.getClass())){
					 KillTask kt = (KillTask) obj;
					 synchronized(pidToCore){
						 if(pidToCore.containsKey(kt.PID)){
							 int i = pidToCore.get(kt.PID);
							 System.out.println("Killing task " + kt.PID);
							 killTask[i]=true;
						 }
					 }
				 }
				 

			 } catch (IOException e) {
				 e.printStackTrace();
				 
			 } catch (ClassNotFoundException e) {
				 e.printStackTrace();
			 }
		}
	}
	
	public void sendTaskFinish(Task t, StatusUpdate.Type type){
		try {
			Socket client = new Socket(master.getAddress(),master.getPort());
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
			System.out.println("WORKS");
			objOut.writeObject(new StatusUpdate(t, type, new InetSocketAddress(InetAddress.getLocalHost(),portnum)));
			
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		synchronized(pidToCore){
			pidToCore.remove(t.PID);
		 }

	}
	
	public void startNewTask(Task t){
		String line;
		
		int coreNum=0;
		synchronized(pidToCore){
			for(int i=0; i<numCores; i++){
				if(!pidToCore.values().contains(i)){
					pidToCore.put(t.PID, i);
					coreNum=i;
					break;
				}
			}
		 }
		killTask[coreNum]=false;
		
		//Create the map reducer
		File file = new File("");
	    URL url=null;
		try {
			url = file.toURL();
		} catch (MalformedURLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	    URL[] urls = new URL[]{url};

	    ClassLoader cl = new URLClassLoader(urls);
	    Class<?> cls;
	    MapReducer mr=null;
	    try{
			cls = cl.loadClass(t.mapReduceClass.substring(0, t.mapReduceClass.lastIndexOf(".")));
			mr=(MapReducer)cls.newInstance();
			mr.equals(mr);
		
		
			 System.out.println("Task " + t.PID + " Started");
			 
			 String mapline;
			if(t.type==Task.Type.MAP){
				 File input= DFSUtil.getFile(this.dfs.masterLoc, t.files.get(0),dfs.fileFolder);
				 if(input == null){
					 sendTaskFinish(t,StatusUpdate.Type.FAILED);
				 }
				 
				 
				 File outputs[] = new File[numPartitions];
				 BufferedWriter bws[] = new BufferedWriter[numPartitions];
				 for(int i =0; i<numPartitions; i++){
					 outputs[i]=new File(dfs.fileFolder + "/tmp/" + t.PID + "-output-" + i+ ".txt");
					 bws[i]=new BufferedWriter(new FileWriter(outputs[i]));
				 }
				 
				 BufferedReader br = new BufferedReader(new FileReader(input));
				 
				while((line = br.readLine()) != null && !killTask[coreNum]){
					mapline=mr.map(line);
					if(mapline!=null)
					{
						BufferedReader bufReader = new BufferedReader(new StringReader(mapline));
						String maplineout=null;
						while( (maplineout=bufReader.readLine()) != null )
						{
							int part= mr.partition(maplineout);
							if(part>numPartitions || numPartitions<0){
								part=0;
								System.out.println("Invalid partion, placing at 0");
							}
							
							bws[part].write(maplineout);
							bws[part].newLine();
						}
					}
				}
				for(int i=0; i<bws.length; i++){
					DFSUtil.createLocalFile(this.dfs.masterLoc, 
						 	new InetSocketAddress(InetAddress.getLocalHost(),this.dfs.portNumber),"tmp/" + t.PID + "-output-" + i+ ".txt");
					bws[i].close();
				}
				br.close();
			}
			else {
				File inputs[] = new File[t.files.size()];
				BufferedReader brs[] = new BufferedReader[numPartitions];
				
				 for(int i =0; i<t.files.size(); i++){
					 inputs[i]=DFSUtil.getFile(this.dfs.masterLoc, t.files.get(i),dfs.fileFolder);
					 //This means that 1 of the map results are lost, we just have to back out to make sure nodes are
					 //Available to service the map
					 if(inputs[i]==null){
						 sendTaskFinish(t,StatusUpdate.Type.FAILED);
						 return;
					 }
				 }
				 line=null;
				 List<String> records = new ArrayList<String>();
				 File out= new File(dfs.fileFolder +"/" + "result-" + t.PID + ".txt");
				 BufferedWriter bw=new BufferedWriter(new FileWriter(out));
				 
				 for(int i=0; i<brs.length; i++){
					 brs[i]=new BufferedReader(new FileReader(inputs[i]));
					 
					 while((line = brs[i].readLine()) !=null && !killTask[coreNum]){
							records = mr.reduce(records,line);
					 }	
				 }

				 for(String r : records){
					 bw.write(r);
					 bw.newLine();
				 }
				 
				 for(int i=0; i<brs.length; i++){
					 brs[i].close();
				 }
				 bw.close();
				 System.out.println("HELLLLLLLLLLLLLLLLLLLLLO");
				 dfs.distributeFile("result-" + t.PID + ".txt");
				 
			}
				
		
		 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		 }catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InstantiationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    if(killTask[coreNum]){
	    	sendTaskFinish(t,StatusUpdate.Type.FAILED);
	    	System.out.println("Task " + t.PID + " Killed");
	    }else{
	    	sendTaskFinish(t,StatusUpdate.Type.TERMINATED);
	    }
	}
		
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new Heartbeat(),3000,1000);
		
		while(true) {
			try {
				Socket client = server.accept();
				System.out.println("GOT Communication");
				//Generate a connection handle and run it in a 
				//separate thread
				ConnectionHandle ch = new ConnectionHandle(client);
				Thread t = new Thread(ch);
				t.start();
				
			} catch (IOException e) {
				
				System.out.println("CONNECTION FAILURE");
			}
			 
		}
	}
	
}
