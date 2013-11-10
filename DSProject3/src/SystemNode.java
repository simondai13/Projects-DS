import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
			
			try{ 
				InputStream in = client.getInputStream();

				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String line = br.readLine();
				
				//starts the appropriate type of node
				if(line.contains("MASTER")){
					
					int portnum = br.read();
					Master m= null;//new Master(portnum);
					Thread t = new Thread(m);
					t.start();
				}
				else if(line.contains("COMPUTE")){
				
					int portnum = br.read();
					ComputeNode compute = new ComputeNode(portnum);
					Thread t = new Thread(compute);
					t.start();
				}
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
