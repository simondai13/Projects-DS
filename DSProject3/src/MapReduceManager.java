import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//primary manager class, to be used by application programmer
public class MapReduceManager {

	
	public void configMapReduce(File configFile){
		
		BufferedReader br = null;
		double maxmaps=1;
		int numPartitions=1;
		int replicationFactor=1;
		long heartbeat=10000;
		long delay = 1000;
		List<InetSocketAddress> participants = new ArrayList<InetSocketAddress>();
		Map<InetSocketAddress, Integer> participantLocations = new TreeMap<InetSocketAddress, Integer>();
		InetSocketAddress masterLocation=null;
		int masterNodePort=0;
		List<File> dataFiles = new ArrayList<File>();
		
		try {
			br = new BufferedReader(new FileReader(configFile));
			String line = "";
			while((line = br.readLine()) != null){
				
				int eqIndex = line.indexOf('=');
				String paramType = line.substring(0, eqIndex);
				String paramValue = line.substring(eqIndex+1);
				String address;
				int port;
				
				switch(paramType){
				
				case "MASTER":
					address = paramValue;
					line = br.readLine();
					port = Integer.parseInt(line.substring(line.indexOf('=')+1));
					line = br.readLine();
					masterNodePort = Integer.parseInt(line.substring(line.indexOf('=')+1));
					masterLocation = new InetSocketAddress(address, port);
					
					break;
				case "PARTICIPANT":
					address = paramValue;
					line = br.readLine();
					port = Integer.parseInt(line.substring(line.indexOf('=')+1));
					line = br.readLine();
					InetSocketAddress adr = new InetSocketAddress(address,  port);
					participants.add(adr);
					participantLocations.put(adr,Integer.parseInt(line.substring(line.indexOf('=')+1)));
					break;
				case "MAXMAPS":
					maxmaps = Double.parseDouble(paramValue);
					break;
				case "DATAFILE":
					String fname = paramValue;
					dataFiles.add(new File(fname));
					break;
				case "REPLICATION":
					replicationFactor = Integer.parseInt(paramValue);
					break;
				case "HEARBEAT":
					heartbeat= Long.parseLong(paramValue);
					break;
				case "DELAY":
					delay = Long.parseLong(paramValue);
					break;
					
					
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int numHosts = participantLocations.size();
		int numFiles = dataFiles.size();
		
		//creates ComputeNodes
		for(InetSocketAddress p : participantLocations.keySet()){
			
			try {
				
				Socket client = new Socket(p.getAddress(), p.getPort());
				String message = "COMPUTE\n"+participantLocations.get(p)+"\n";
				PrintWriter out = new PrintWriter(client.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out.write(message);
				String response = in.readLine();
				client.close();
				if(response == null || response.equals("FAIL"))
					throw new IOException();
			} catch (IOException e) {

				numHosts--;
				participantLocations.remove(p);
				participants.remove(p);
				System.out.println("Participant " + p.toString() + " unavailable, will be removed from system");
				e.printStackTrace();
			}
		}

		//set up master
		if(masterLocation == null)
			System.out.println("No Master node provided");
		
		try {
			
			Socket client = new Socket(masterLocation.getAddress(), masterLocation.getPort());
			String message = "MASTER\n"+masterNodePort+"\n"+heartbeat+"\n"+delay+"\n";
			PrintWriter out = new PrintWriter(client.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out.write(message);
			out.close();
			String response = in.readLine();
			in.close();
			client.close();
			if(response == null || response.equals("FAIL"))
				throw new IOException();
		} catch (IOException e) {
			System.out.println("Master Setup Failed");
		}
		
		
		//give ComputeNodes their files, also tell master where they are
		for(int i = 0; i < numFiles; i++){

			File f = dataFiles.get(i);
			try {
				int numLines = 0;
				BufferedReader lineCount = new BufferedReader(new FileReader(f));
				while(lineCount.readLine() != null){
					numLines++;
				}
				
				BufferedReader readFile = new BufferedReader(new FileReader(f));
				
				
				
			} catch (FileNotFoundException e) {

				System.out.println("File " + f.getName() + " not found");
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
