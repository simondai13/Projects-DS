import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class DFSNode implements Runnable{

	private ServerSocket server;
	public InetSocketAddress masterLoc;
	public MapReducer mr;
	public int portNumber;
	String fileFolder;
	
	public DFSNode(InetSocketAddress masterLoc, int portnum) throws IOException{
		
		server = new ServerSocket(portnum);
		this.masterLoc = masterLoc;
		this.portNumber=portnum;
		fileFolder=String.valueOf(portnum);
		File f = new File(fileFolder);
		if(!f.exists())
			f.mkdir();
		File f2 = new File(fileFolder+"/tmp");
		if(!f2.exists())
			f2.mkdir();
		
	}
	
	//Distribute a file over the specified nodes
	public List<InetSocketAddress> getReplLocs(String filename){
		List<InetSocketAddress> destinations = new ArrayList<InetSocketAddress>();
		try {
			Socket master = new Socket(masterLoc.getAddress(),masterLoc.getPort());
			PrintWriter out = new PrintWriter(master.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(master.getInputStream()));
			out.println("NEWFILEREQ\n" + filename);
			out.flush();
			String res="";
			System.out.println("WHAT");
			while((res=in.readLine())!=null){
				int port = Integer.parseInt(in.readLine());
				destinations.add(new InetSocketAddress(res,port));
			}
			System.out.println("Works");
			in.close();
			out.close();
			master.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return destinations;
	}
	
	//Distribute a file over the nodes specified by the master
	public void distributeFile(String filename){
		distributeFile(filename,getReplLocs(filename));
	}
	
	//assume local file exists, makes it distributed
	public void distributeFile(String filename, List<InetSocketAddress> destinations){
		System.out.println("Sending file around to " + destinations);
		//send copies to appropriate nodes
		for(InetSocketAddress d : destinations){
			
			Socket s;
			try {
				s = new Socket(d.getAddress(), d.getPort());
				PrintWriter out = new PrintWriter(s.getOutputStream());
				DFSUtil.sendFile(out, filename,fileFolder);
				out.close();
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	public BufferedReader read(String filename) throws FileNotFoundException{
		
		return new BufferedReader(new FileReader(getFile(filename)));
	}
	
	private File getFile(String filename){
		File f=null;
		try {
			f= DFSUtil.getFile(masterLoc, filename, fileFolder);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	}
	
	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		@Override
		public void run() {
			try{ 
				 PrintWriter out = new PrintWriter(client.getOutputStream());
				 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				 
				 String line = in.readLine();
				 //node is receiving a request for a file
				 if(line!=null && line.contains("FILEREQUEST")){
					 String filename = in.readLine();
					 System.out.println("got request for" + filename);
					 DFSUtil.sendFile(out, filename, fileFolder);
				 }
				 //node is receiving a file
				 else if(line!=null && (line.contains("FILESEND") || line.contains("CLASSFILE"))){
					 
					String filename = in.readLine();
					File f = new File(filename);
					f.createNewFile();
					PrintWriter fileWriter = new PrintWriter(fileFolder+ "/"+ f);
					String fileLine = "";
					while((fileLine = in.readLine()) != null){
						
						fileWriter.println(fileLine);
					}
					fileWriter.close();
					System.out.println("File " + filename + " replicated on " + 
								InetAddress.getLocalHost().toString() +":" + client.getLocalPort());			

				 }
				 
				 out.close();
				 in.close();
			 } catch (IOException e) {
				 e.printStackTrace();
			 } 
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
				
				System.out.println("CONNECTION FAILURE");
			}
			 
		}
	}
}
