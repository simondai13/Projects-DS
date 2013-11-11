import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ComputeNode implements Runnable{

	
	//files on this compute node
	private FileSystemNode fileSystem;
	private ServerSocket server;
	
	
	public ComputeNode(int portnum) throws IOException{
		
		
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
	
	private File getFile(){
		
		return null;
	}
}
