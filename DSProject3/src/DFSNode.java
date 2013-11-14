import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


public class DFSNode implements Runnable{

	private ServerSocket server;
	private InetSocketAddress masterLoc;
	
	public DFSNode(InetSocketAddress masterLoc, int portnum) throws IOException{
		
		server = new ServerSocket(portnum);
		this.masterLoc = masterLoc;
	}
	
	//assume local file exists, makes it distributed
	public void distributeFile(String filename){
		
		//tell master, get locations 
		List<InetSocketAddress> destinations = new ArrayList<InetSocketAddress>();
		try {
			Socket master = new Socket(masterLoc.getAddress(),masterLoc.getPort());
			PrintWriter out = new PrintWriter(master.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(master.getInputStream()));
			out.println("NEWFILEREQ\n" + filename);
			String res="";
			while((res=in.readLine())!=null){
				int port = Integer.parseInt(in.readLine());
				destinations.add(new InetSocketAddress(res,port));
			}
			
			in.close();
			out.close();
			master.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		//send copies to appropriate nodes
		for(InetSocketAddress d : destinations){
			
			Socket s;
			try {
				s = new Socket(d.getAddress(), d.getPort());
				PrintWriter out = new PrintWriter(s.getOutputStream());
				DFSUtil.sendFile(out, filename);
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
		
		File f = new File(filename);
		File f2 = new File("tmp/"+filename);
		
		if(!f.exists() && !f2.exists()){
			
			//get temp copy
			try {
				return DFSUtil.getFile(masterLoc, filename);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(f.exists())
			return f;
		
		return f2;
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
				 if(line.contains("FILEREQUEST")){
					 
					 String filename = in.readLine();
					 DFSUtil.sendFile(out, filename);
				 }
				 //node is receiving a file
				 else if(line.contains("FILESEND")){
					 
					String filename = in.readLine();
					File f = new File(filename);
					if(!f.createNewFile()){
						//fail
					}
					PrintWriter fileWriter = new PrintWriter(f);
					String fileLine = "";
					while((fileLine = in.readLine()) != null){
						
						fileWriter.println(fileLine);
					}
					fileWriter.close();
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
