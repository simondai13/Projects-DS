import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class DFSUtil {


	//writes to end
	public static void write(String filename, String writeContent) throws IOException{
		
		BufferedWriter output = new BufferedWriter(new FileWriter(filename, true));
		output.write(writeContent);
		output.close();
	}

	//Compute node uses this static method to get a temp copy of a distributed file
	//GETS a file from other node, copies
	public static void copyFileRead(InetSocketAddress fileLoc, String filename) throws UnknownHostException, IOException{
		
		String message = "FILEREQUEST\n"+filename+"\n";
		Socket fileReq = new Socket(fileLoc.getHostName(), fileLoc.getPort());
		BufferedReader in = new BufferedReader(new InputStreamReader(fileReq.getInputStream()));
		PrintWriter out = new PrintWriter(fileReq.getOutputStream());
		
		out.write(message);
		File tempCopy = new File("tmp/"+filename);
		if(!tempCopy.createNewFile()){
			throw new IOException();
		}
		String line="";
		PrintWriter out2 = new PrintWriter(new FileWriter(tempCopy));
		while((line=in.readLine())!=null){
			
			out2.println(line);
		}
	}
	
	//Compute node uses this static method AFTER it receives a request for a file on its machine
	//SENDS a file TO some other node
	public static void copyFileWrite(PrintWriter out, String filename) throws IOException {
		
		String message = "FILESEND\n"+filename+"\n";
		
		//reads the file, writes it to destination
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = "";
		out.println(message);
		while((line = in.readLine()) != null){
			
			out.println(line);
		}
		
		in.close();
	}
}
