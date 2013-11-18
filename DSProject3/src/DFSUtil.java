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
	public static File getFile(InetSocketAddress masterLoc, String filename, String folder) throws UnknownHostException, IOException{
		
		//See if the file is stored locally on this machine
		File in1=new File(folder+ "/" + filename);
		File in2=new File(folder+ "/tmp/" + filename);
		if(in1.exists())
			return in1;
		if(in2.exists())
			return in2;
		
		//get host location from master
		String message = "FILELOCATION\n" + filename;
		Socket master = new Socket(masterLoc.getHostName(), masterLoc.getPort());

		BufferedReader masterin = new BufferedReader(new InputStreamReader(master.getInputStream()));
		PrintWriter masterout = new PrintWriter(master.getOutputStream());
		masterout.println(message);
		masterout.flush();

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
				System.out.println("getting file");
				out.write(message);
				out.flush();
				File tempCopy = new File(folder +"/tmp/"+filename.substring(0,filename.lastIndexOf("/")));
				tempCopy.createNewFile();
				String line="";
				PrintWriter out2 = new PrintWriter(new FileWriter(tempCopy));
				while((line=in.readLine())!=null){	
					out2.println(line);
				}
				System.out.println("Got file");
				out2.flush();
				fileReq.close();
				in.close();
				out.close();
				out2.close();
				return tempCopy;
			} catch (IOException e) {
				e.printStackTrace();
				//try next host
			}
		}
		return null;
	}
	
	public static void createLocalFile(InetSocketAddress masterLoc, InetSocketAddress localAddr, String filename) throws IOException{
		
		Socket master = new Socket(masterLoc.getHostName(), masterLoc.getPort());

		PrintWriter masterout = new PrintWriter(master.getOutputStream());
		masterout.println("NEWFILE");
		masterout.println(filename);
		masterout.println(localAddr.getHostName());
		masterout.println(localAddr.getPort());
		masterout.flush();
		masterout.close();
		master.close();
	}
	
	public static void sendFile(PrintWriter out, String filename, String folder, String tag) throws IOException {
		String message = tag + "\n"+filename+"\n";
		
		//reads the file, writes it to destination
		BufferedReader in = new BufferedReader(new FileReader(folder + "/" + filename));
		String line = "";
		out.println(message);
		while((line = in.readLine()) != null){
			
			out.println(line);
		}
		
		in.close();
	}
	
	//Compute node uses this static method AFTER it receives a request for a file on its machine
	//SENDS a file TO some other node
	public static void sendFile(PrintWriter out, String filename, String folder) throws IOException {
		sendFile(out,filename,folder,"FILESEND");
	}
	
	
	
}
