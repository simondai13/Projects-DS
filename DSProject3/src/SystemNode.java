import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

//represents a node in the mapreduce system, 
//waits for config requests to start compute/master nodes
public class SystemNode implements Runnable{

	private ServerSocket server;
	
	public SystemNode(int portnum) throws IOException{
		
		server = new ServerSocket(portnum);
	}

	//handles connections
	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		@Override
		public void run() {
			
			PrintWriter out = null;
			BufferedReader in = null;
			String line = null;
			try{ 
				out = new PrintWriter(client.getOutputStream());
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				line = in.readLine();

			 } catch (IOException e) {
				 e.printStackTrace();
			 }
				
			try{
				if(line == null)
					out.println("FAIL");
				//starts the appropriate type of node
				else if(line.contains("MASTER")){
						
					int portnum = Integer.parseInt(in.readLine());
					int dfsPort = Integer.parseInt(in.readLine());
					int replicationFactor = Integer.parseInt(in.readLine());
					int numCores = Integer.parseInt(in.readLine());
					int numPartitions = Integer.parseInt(in.readLine());
					Master m= new Master(portnum, dfsPort, replicationFactor,numCores,numPartitions);
					Thread t = new Thread(m);
					t.start();
					out.println("OK");
					out.flush();
					System.out.println("Master initiated on " + InetAddress.getLocalHost().toString() +":" + portnum);
				}
				else if(line.contains("COMPUTE")){
				
					int portnum = Integer.parseInt(in.readLine());
					int filePortnum = Integer.parseInt(in.readLine());
					InetAddress masterAdr = InetAddress.getByName(in.readLine());
					int masterPort = Integer.parseInt(in.readLine());
					int masterDFSPort = Integer.parseInt(in.readLine());
					int numCores = Integer.parseInt(in.readLine());
					int numPartitions = Integer.parseInt(in.readLine());
					DFSNode node = new DFSNode(new InetSocketAddress(masterAdr,masterDFSPort), filePortnum);
					Thread t = new Thread(node);
					t.start();
					ComputeNode compute = new ComputeNode(portnum,new InetSocketAddress(masterAdr,masterPort),node,numCores,numPartitions);
					Thread tCompute = new Thread(compute);
					tCompute.start();
					out.println("OK");
					out.flush();
					System.out.println("Worker initiated on " + InetAddress.getLocalHost().toString()+ ":"+portnum);
				}
			}catch(IOException e){
				e.printStackTrace();
				out.println("FAIL");			
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
