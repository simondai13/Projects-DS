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
import java.util.Set;
import java.util.TreeMap;

public class MasterDFS implements Runnable{

	private ServerSocket server;
	public TreeMap<String,List<InetSocketAddress>> fileLocs;
	public List<InetSocketAddress> fileNodes;
	private Master master;
	private int replFactor;
	
	public MasterDFS(Master master, int portnum, int replFactor) throws IOException{
		
		server = new ServerSocket(portnum);
		fileLocs = new TreeMap<String, List<InetSocketAddress>>();
		this.master=master;
		this.replFactor=replFactor;
		fileNodes=new ArrayList<InetSocketAddress>();
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
					 System.out.println("FileLocation Request made for " + filename);
					 String message = "";
					 synchronized(fileLocs){
						 List<InetSocketAddress> locations = fileLocs.get(filename);
						 if(locations!=null){
							 for(InetSocketAddress adr : locations){
								 message += adr.getHostName()+"\n";
								 message += adr.getPort()+"\n";
							 }
						 }
					 }
					 out.print(message);
					 out.flush();
				 }
				 //node is receiving a file
				 else if(line.contains("NEWFILE") && !line.contains("REQ")){
					String filename = in.readLine();
					List<InetSocketAddress> locations = new ArrayList<InetSocketAddress>();
					while((line=in.readLine())!=null){
						InetAddress hostadr = InetAddress.getByName(line);
						int hostPort = Integer.parseInt(in.readLine());
						locations.add(new InetSocketAddress(hostadr, hostPort));
					}
					synchronized(fileLocs){
						fileLocs.put(filename, locations);
					}
				 //Node is requesting all locations so that the MapReduce.class can be copied to all nodes
				 }else if (line.contains("NODELOCATIONS")){
						while((line=in.readLine())!=null){
							InetAddress hostadr = InetAddress.getByName(line);
							int hostPort = Integer.parseInt(in.readLine());
							fileNodes.add(new InetSocketAddress(hostadr, hostPort));
						}
				 }
				 else if (line.contains("CLASSFILE")){
					 for(InetSocketAddress addr : fileNodes){
						 if(!addr.equals((InetSocketAddress) client.getRemoteSocketAddress())){
							out.println(addr.getHostName());
						 	out.println(addr.getPort());
					 	}
					 }
				 //Node is requesting locations of where to replicate new files, simply send 3 random
				 //addresses
				 }else if (line.contains("NEWFILEREQ")){
					 
					 String filename = in.readLine();
					 int r = (int) (Math.random() *master.activeNodes.size());
					 int j=replFactor-1; //We already have a copy on the FILEREQ node
					 //it is possible we lost enough nodes that the replication factor can't be satisfied
					 j=Math.min(j,master.activeNodes.size()-1);
					 int i=0;
					 List<InetSocketAddress> newLocs = new ArrayList<InetSocketAddress>();
					 while(i<j){
						 InetSocketAddress n=fileNodes.get((r+i)%fileNodes.size());
						 if(!n.equals((InetSocketAddress) client.getRemoteSocketAddress()) &&
							master.isActiveFileNode(n)){
							out.println(n.getHostName());
					     	out.println(n.getPort());
					     	newLocs.add(n);
						 }else{
							j++;
						 }
						 
						 i++;
					 }
					 out.flush();
					 synchronized(fileLocs){
						 fileLocs.put(filename, newLocs);
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
