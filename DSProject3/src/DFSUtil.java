import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DFSUtil {

	//Compute node uses this static method to get a temp copy of a distributed file
	//GETS a file from other node, copies
	public static File getFile(InetSocketAddress masterLoc, String filename) throws UnknownHostException, IOException{
		
		//get host location from master
		
		String message = "FILELOCATION\n" + filename+"\n";
		Socket master = new Socket(masterLoc.getHostName(), masterLoc.getPort());

		BufferedReader masterin = new BufferedReader(new InputStreamReader(master.getInputStream()));
		PrintWriter masterout = new PrintWriter(master.getOutputStream());
		masterout.println(message);

		List<InetSocketAddress> fileLocations = new ArrayList<InetSocketAddress>();
		
		String reply = "";
		while((reply = masterin.readLine()) != null){
			
			int portnum = Integer.parseInt(masterin.readLine());
			fileLocations.add(new InetSocketAddress(reply, portnum));
		}
		
		masterin.close();
		masterout.close();
		master.close();
		//get the file
		for(InetSocketAddress fileLoc : fileLocations){
			
			try {
				message = "FILEREQUEST\n"+filename+"\n";
				Socket fileReq = new Socket(fileLoc.getHostName(), fileLoc.getPort());
				BufferedReader in = new BufferedReader(new InputStreamReader(fileReq.getInputStream()));
				PrintWriter out = new PrintWriter(fileReq.getOutputStream());
				
				out.write(message);
				File tempCopy = new File("tmp/"+filename);
				tempCopy.createNewFile();
				String line="";
				PrintWriter out2 = new PrintWriter(new FileWriter(tempCopy));
				while((line=in.readLine())!=null){
					
					out2.println(line);
				}
				
				fileReq.close();
				in.close();
				out.close();
				out2.close();
				return tempCopy;
			} catch (IOException e) {
				//try next host
			}
		}
		return null;
	}
	
	public static void sendFile(PrintWriter out, String filename, String tag) throws IOException {
		String message = tag + "\n"+filename+"\n";
		
		//reads the file, writes it to destination
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = "";
		out.println(message);
		while((line = in.readLine()) != null){
			
			out.println(line);
		}
		
		in.close();
	}
	
	//Compute node uses this static method AFTER it receives a request for a file on its machine
	//SENDS a file TO some other node
	public static void sendFile(PrintWriter out, String filename) throws IOException {
		sendFile(out,filename,"FILESEND");
	}
	
	
	
}
