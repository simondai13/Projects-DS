import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
		int fileSplit = -1;
		long heartbeat=10000;
		long delay = 1000;
		List<InetSocketAddress> participants = new ArrayList<InetSocketAddress>();
		Map<InetSocketAddress, Integer> participantLocations = new TreeMap<InetSocketAddress, Integer>();
		Map<InetSocketAddress, Integer> participantFileLocations = new TreeMap<InetSocketAddress, Integer>();
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
					line = br.readLine();
					participantFileLocations.put(adr,Integer.parseInt(line.substring(line.indexOf('=')+1)));
					
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
				case "FILESPLIT":
					fileSplit = Integer.parseInt(paramValue);
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
		if(fileSplit == -1)
			fileSplit = numHosts;
		int numFiles = dataFiles.size();

		//set up master
		if(masterLocation == null)
			System.out.println("No Master node provided");
		
		try {
			
			Socket client = new Socket(masterLocation.getAddress(), masterLocation.getPort());
			String message = "MASTER\n"+masterNodePort+"\n"+heartbeat+"\n"+delay+"\n";
			PrintWriter out = new PrintWriter(client.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out.println(message);
			out.close();
			String response = in.readLine();
			in.close();
			client.close();
			if(response == null || response.equals("FAIL"))
				throw new IOException();
		} catch (IOException e) {
			System.out.println("Master Setup Failed");
		}
		
		//creates ComputeNodes
		for(InetSocketAddress p : participantLocations.keySet()){
			
			try {
				
				Socket client = new Socket(p.getAddress(), p.getPort());
				String message = "COMPUTE\n"+participantLocations.get(p)+"\n"+participantFileLocations.get(p)+"\n"+
								masterLocation.getHostName()+"\n"+masterLocation.getPort()+"\n";
				PrintWriter out = new PrintWriter(client.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out.println(message);
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
		//give ComputeNodes their files, also tell master where they are
		for(int i = 0; i < numFiles; i++){

			File f = dataFiles.get(i);
			try {
				int numLines = 0;
				BufferedReader lineCount = new BufferedReader(new FileReader(f));
				while(lineCount.readLine() != null){
					numLines++;
				}
				lineCount.close();
				BufferedReader readFile = new BufferedReader(new FileReader(f));
				
				for(int j = 0; j < fileSplit; j++){
					
					Socket[] fileRecipients = new Socket[replicationFactor];
					PrintWriter[] outStreams = new PrintWriter[replicationFactor];
					List<InetSocketAddress> fileCopies = new ArrayList<InetSocketAddress>();
					for(int k = 0; k < replicationFactor; k++){
						
						InetSocketAddress p = participants.get((k+j) % numHosts);
						fileCopies.add(p);
						fileRecipients[k] = (new Socket(p.getHostName(), participantFileLocations.get(p)));
						outStreams[k] = new PrintWriter(fileRecipients[k].getOutputStream());
					}
					
					//tell master who has part j of the file
					String fname = f.getName();
					fileCopies = null;
					
					//write file out to given nodes
					for(int k = 0; k < numLines/fileSplit; k++){
						
						String line = readFile.readLine();
						for(int l = 0; l < replicationFactor; l++){
							
							outStreams[l].println(line);
						}
						outStreams[k].close();
						fileRecipients[k].close();
					}
					
				}
				readFile.close();
				
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
