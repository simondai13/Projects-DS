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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
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
	//Simple data structure that does not need to be locked,
	//so it can be quickly checked each iteration of the map/reduce task
	volatile boolean[] killTask; 
	Map<Integer,Integer> pidToCore;
	
	public ComputeNode(int portnum, InetSocketAddress master,DFSNode dfs, int numCores) throws IOException{
		this.portnum=portnum;
		this.master=master;
		this.numCores=numCores;
		server = new ServerSocket(portnum);
		this.dfs=dfs;
		killTask=new boolean[numCores];
		pidToCore=new TreeMap<Integer,Integer>();
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
				 System.out.println("WORKS3");
				 
				 //Make sure this message is packed properly
				 if(Task.class.isAssignableFrom(obj.getClass()))
				 {
					 Task t = (Task) obj;
					 
					 startNewTask(t);	
				 }
				 else if(KillTask.class.isAssignableFrom(obj.getClass())){
					 KillTask kt = (KillTask) obj;
					 System.out.println("WORKSSDFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
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
			 File input=new File(t.file);
			 if(!input.exists()){
				 input = DFSUtil.getFile(this.dfs.masterLoc, t.file);
			 }
			 
			 File output = new File(t.PID + "-output.tmp");
			 BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			 BufferedReader br = new BufferedReader(new FileReader(input));
			 
			 String mapline;
			if(t.type==Task.Type.MAP){
				while((line = br.readLine()) != null && !killTask[coreNum]){
					mapline=mr.map(line);
					if(mapline!=null)
					{
						bw.write(mapline);
						bw.newLine();
					}
				}
			}
			else {
				String[] mapInput = new String[2];
				String[] mapOutput = new String[2];
				mapOutput[1] = br.readLine();
				mapOutput[0] = null;
				while((line = br.readLine()) !=null && !killTask[coreNum]){
					mapInput[0]=mapOutput[1];
					mapInput[1]=line;
					mapOutput = mr.reduce(mapInput);
					
					if(mapOutput[0]!=null){
						bw.write(mapOutput[0]);
						bw.newLine();
					}
				}
				
				if(mapOutput[1]!=null)
					bw.write(mapOutput[1]);
				
			}
			
			br.close();
			bw.close();
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
