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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ComputeNode implements Runnable{

	
	//files on this compute node
	private FileSystemNode fileSystem;
	private ServerSocket server;
	private InetSocketAddress master;
	private DFSNode dfs;
	private int portnum;
	
	public ComputeNode(int portnum, InetSocketAddress master,DFSNode dfs) throws IOException{
		this.portnum=portnum;
		this.master=master;
		server = new ServerSocket(portnum);
		fileSystem = new FileSystemNode();
		this.dfs=dfs;
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
				 
				 //Simply send a dummy message back to the master affirming we got this task
				 //objOut.writeObject(obj);

			 } catch (IOException e) {
				 
			 } catch (ClassNotFoundException e) {
				   
			 }
		}
	}
	
	public void sendTaskFinish(Task t){
		System.out.println("WORKS");
		try {
			Socket client = new Socket(master.getAddress(),master.getPort());
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
			System.out.println("WORKS");
			objOut.writeObject(new StatusUpdate(t, StatusUpdate.Type.TERMINATED, new InetSocketAddress(InetAddress.getLocalHost(),portnum)));
			
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(master.getPort() +"MASTA PORT");

	}
	
	public void startNewTask(Task t){
		String line;
		//Create the map reducer
		File file = new File(""); //current directory
	    // Convert File to a URL
	    URL url=null;
		try {
			url = file.toURL();
		} catch (MalformedURLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	    URL[] urls = new URL[]{url};

	    // Create a new class loader with the directory
	    ClassLoader cl = new URLClassLoader(urls);

	    // Load in the class; MyClass.class should be located in
	    // the directory file:/c:/myclasses/com/mycompany
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
				while((line = br.readLine()) != null){
					mapline=mr.map(line);
					if(mapline!=null)
					{
						bw.write(mapline);
						bw.newLine();
					}
				}
			}
			else {
				ArrayList<String> lines = new ArrayList<String>();
				while((line = br.readLine()) !=null ){
					lines.add(line);
				}
				System.out.println("Works");
				String[] lineArray = new String[lines.size()];
				lines.toArray(lineArray);
				String[] resultLines= mr.reduce(lineArray);
				
				if(resultLines!=null){
					//This should be changed to write to the distributed system
					for(int i=0; i<resultLines.length; i++){
						bw.write(lineArray[i]);
						bw.newLine();
					}
				}
				
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
	    sendTaskFinish(t);
	}
		
	@Override
	public void run() {
		// TODO Auto-generated method stub

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
