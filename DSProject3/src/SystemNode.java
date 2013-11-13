import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

//represents a node in the mapreduce system, 
//waits for config requests to start compute/master nodes
public class SystemNode implements Runnable{

	private ServerSocket server;
	
	public SystemNode(int portnum) throws IOException{
		
		server = new ServerSocket(portnum);
	}

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
			 }
				
			try{
				if(line == null)
					out.println("FAIL");
				//starts the appropriate type of node
				else if(line.contains("MASTER")){
						
					int portnum = Integer.parseInt(in.readLine());
					long heartbeat = Long.parseLong(in.readLine());
					long delay = Long.parseLong(in.readLine());
					Master m= null;//new Master(portnum);
					Thread t = new Thread(m);
					t.start();
				}
				else if(line.contains("COMPUTE")){
				
					int portnum = Integer.parseInt(in.readLine());
					ComputeNode compute = new ComputeNode(portnum);
					Thread t = new Thread(compute);
					t.start();
					out.println("OK");
				}
			}catch(IOException e){
				
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
