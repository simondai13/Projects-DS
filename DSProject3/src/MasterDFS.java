import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class MasterDFS implements Runnable{

	private ServerSocket server;
	private TreeMap<String,List<InetSocketAddress>> fileLocs;
	private Master master;
	private int replFactor;
	
	public MasterDFS(Master master, int portnum, int replFactor) throws IOException{
		
		server = new ServerSocket(portnum);
		fileLocs = new TreeMap<String, List<InetSocketAddress>>();
		this.master=master;
		this.replFactor=replFactor;
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
				 if(line.contains("FILELOCATION")){
					 
					 String filename = in.readLine();
					 List<InetSocketAddress> locations = fileLocs.get(filename);
					 String message = "";
					 for(InetSocketAddress adr : locations){
						 message += adr.getHostName()+"\n";
						 message += adr.getPort()+"\n";
					 }
					 
					 out.print(message);
				 }
				 //node is receiving a file
				 else if(line.contains("NEWFILE")){
					 
					String filename = in.readLine();
					
					List<InetSocketAddress> locations = new ArrayList<InetSocketAddress>();
					while((line=in.readLine())!=null){
						
						InetAddress hostadr = InetAddress.getByName(line);
						int hostPort = Integer.parseInt(in.readLine());
						locations.add(new InetSocketAddress(hostadr, hostPort));
					}
					fileLocs.put(filename, locations);
				 }else if (line.contains("CLASSFILE")){
					 for(InetSocketAddress addr : master.activeFileNodes){
						 out.println(addr.getHostName());
						 out.println(addr.getPort());
					 }
				 }else if (line.contains("NEWFILEREQ")){
					 
					 String filename = in.readLine();
					 int r = (int) (Math.random() *master.activeFileNodes.size());
					 for(int i=0; i<replFactor; i++){
						 InetSocketAddress n=master.activeFileNodes.get((r+i)%master.activeFileNodes.size());
						 out.println(n.getHostName());
					     out.println(n.getPort());
					 }
					 
				 }
				 
				 out.close();
				 in.close();
			 } catch (IOException e) {
				 
			 }
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub

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
