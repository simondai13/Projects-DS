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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ComputeNode implements Runnable{

	
	//files on this compute node
	private FileSystemNode fileSystem;
	private ServerSocket server;
	private MapReducer mapRed;
	
	public ComputeNode(int portnum, MapReducer mapRed) throws IOException{
		
		server = new ServerSocket(portnum);
		fileSystem = new FileSystemNode();
		this.mapRed=mapRed;
	}


	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		@Override
		public void run() {
			try{ 
				 OutputStream out = client.getOutputStream();
				 ObjectOutput objOut = new ObjectOutputStream(out);
				 
				 InputStream in = client.getInputStream();
				 ObjectInput objIn = new ObjectInputStream(in);
				 Object obj = objIn.readObject();
				 
				 //Make sure this message is packed properly
				 if(Task.class.isAssignableFrom(obj.getClass()))
				 {
					 Task t = (Task) obj;
					 
					 startNewTask(t);	
				 }
				 
				 //Simply send a dummy message back to the master affirming we got this task
				 objOut.writeObject(obj);

			 } catch (IOException e) {
				 
			 } catch (ClassNotFoundException e) {
				   
			 }
		}
	}
	
	public void startNewTask(Task t){
		String line;
		 try {
			 //TODODODODODOD 
			 //Here we should use the distributed files system instead
			 File f = new File(t.file);	
			 
			 File output = new File(t.PID + "-output.tmp");
			 BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			 BufferedReader br = new BufferedReader(new FileReader(f));
			 
			if(t.type==Task.Type.MAP){
				while((line = br.readLine()) != null){
					bw.write(mapRed.map(line));
					bw.newLine();
				}
			}
			else {
				ArrayList<String> lines = new ArrayList<String>();
				while((line = br.readLine()) !=null ){
					lines.add(line);
				}
				String[] lineArray = new String[lines.size()];
				lines.toArray(lineArray);
				String[] resultLines= mapRed.reduce(lineArray);
				
				//This should be changed to write to the distributed system
				for(int i=0; i<resultLines.length; i++){
					bw.write(lineArray[i]);
					bw.newLine();
				}
				
			}
			
			br.close();
			bw.close();
		 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
