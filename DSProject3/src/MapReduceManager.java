import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



//primary manager class, to be used by application programmer
public class MapReduceManager {
	
	InetSocketAddress masterLocation;
	List<InetSocketAddress> participants;
	Map<InetSocketAddress, Integer> participantLocations; 
	Map<InetSocketAddress, Integer> participantFileLocations;
	List<File> dataFiles; 
	int masterNodePort;
	int masterDFSPort;
	
	public MapReduceManager(){
		participants = new ArrayList<InetSocketAddress>();
		participantLocations = new TreeMap<InetSocketAddress, Integer>(new NodeCompare());
		participantFileLocations = new TreeMap<InetSocketAddress, Integer>(new NodeCompare());
		masterLocation=null;
		masterNodePort=0;
		masterDFSPort=0;
	}
	public void startMapReduce(final Class<?> pClass){
	    final String location, name;
	    name = pClass.getName().replaceAll("\\.", "/") + ".class";
	    location = ClassLoader.getSystemResource(name).getPath();

	    if(masterLocation!=null)
	    {
	    	ArrayList<InetSocketAddress> destinations = new ArrayList<InetSocketAddress>();
	    	try{
		    	Socket client = new Socket(masterLocation.getAddress(),masterDFSPort);
		    	String message = "CLASSFILE\n";
		    	PrintWriter out = new PrintWriter(client.getOutputStream());
		    	BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		    	out.println(message);
		    	out.flush();
		    	String res="";
		    	while((res=in.readLine())!=null){
		    		int port = Integer.parseInt(in.readLine());
		    		destinations.add(new InetSocketAddress(res,port));
		    	}
		    	client.close();
	    	}catch (IOException e){
	    		e.printStackTrace();
	    	}
	    	
	    	for(InetSocketAddress d : destinations){
				
				Socket s;
				try {
					s = new Socket(d.getAddress(), d.getPort());
					PrintWriter out = new PrintWriter(s.getOutputStream());
					String message = "CLASSFILE\n"+location.substring(location.lastIndexOf("/")+1)+"\n";
					
					//reads the file, writes it to destination
					BufferedReader in = new BufferedReader(new FileReader(location));
					String line = "";
					out.println(message);
					while((line = in.readLine()) != null){
						
						out.println(line);
					}
					
					in.close();
					out.close();
					s.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	    	
	    	//Tell the master to start executing
		    MasterControlMsg mc=new MasterControlMsg();
		    mc.type=MasterControlMsg.Type.START;
		    mc.mapReduce=location.substring(location.lastIndexOf("/")+1);
		    
		    try
			{
				Socket client = new Socket(masterLocation.getAddress(),masterNodePort);
				OutputStream out = client.getOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(out);
				
				objOut.writeObject(mc);

				client.close();
				
			} catch (IOException e) {
				System.out.println("Error: Unable to connect to Master");
			} 
	    	
		}
	}
	
	public void configMapReduce(File configFile){
		
		BufferedReader br = null;
		int replicationFactor=1;
		int fileSplit = -1;
		long heartbeat=10000;
		long delay = 1000;
		dataFiles = new ArrayList<File>();
		
		try {
			br = new BufferedReader(new FileReader(configFile));
			String line = "";
			while((line = br.readLine()) != null){
				
				int eqIndex = line.indexOf('=');
				String paramType = line.substring(0, eqIndex);
				String paramValue = line.substring(eqIndex+1);
				if(paramValue.contains("localhost")){
					paramValue=InetAddress.getLocalHost().getCanonicalHostName();
				}
				String address;
				int port;
				
				if(paramType.contains("MASTER")){
					address = paramValue;
					line = br.readLine();
					port = Integer.parseInt(line.substring(line.indexOf('=')+1).trim());
					line = br.readLine();
					masterNodePort = Integer.parseInt(line.substring(line.indexOf('=')+1).trim());
					line = br.readLine();
					masterDFSPort = Integer.parseInt(line.substring(line.indexOf('=')+1).trim());
					masterLocation = new InetSocketAddress(address, port);

				}else if (paramType.contains("PARTICIPANT")){
					address = paramValue;
					line = br.readLine();
					port = Integer.parseInt(line.substring(line.indexOf('=')+1).trim());
					line = br.readLine();
					InetSocketAddress adr = new InetSocketAddress(address,  port);
					participants.add(adr);
					participantLocations.put(adr,Integer.parseInt(line.substring(line.indexOf('=')+1).trim()));
					line = br.readLine();
					participantFileLocations.put(adr,Integer.parseInt(line.substring(line.indexOf('=')+1).trim()));
					
				}else if(paramType.contains("DATAFILE")){
					String fname = paramValue;
					dataFiles.add(new File(fname.trim()));
				}else if(paramType.contains("REPLICATION")){
					replicationFactor = Integer.parseInt(paramValue.trim());
				}else if(paramType.contains("HEARTBEAT")){
					heartbeat= Long.parseLong(paramValue.trim());
				}else if(paramType.contains("DELAY")){
					delay = Long.parseLong(paramValue.trim());
				}else if(paramType.contains("MAXFILESPLIT")){
					fileSplit = Integer.parseInt(paramValue.trim());
				}else {
					System.out.println("Invalid configuration file, canceling initiation");
					br.close();
					return;
				}
					
			}
			
			br.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int numHosts = participantLocations.size();
		if(fileSplit == -1)
			fileSplit = numHosts;
		int numFiles = dataFiles.size();

		//set up master
		if(masterLocation == null){
			System.out.println("No Master node provided, System Aborted");
			return;
		}
		try {
			
			Socket client = new Socket(masterLocation.getAddress(), masterLocation.getPort());
			String message = "MASTER\n"+masterNodePort+"\n"+masterDFSPort+"\n"+heartbeat+"\n"+delay+"\n"+replicationFactor;
			PrintWriter out = new PrintWriter(client.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			out.println(message);
			out.flush();
			
			String response = in.readLine();
			
			out.close();
			in.close();
			client.close();
			if(response == null || response.equals("FAIL"))
				throw new IOException();
		} catch (IOException e) {
			System.out.println("Unable to connect to Master Node");
			System.out.println("Master Setup Failed, System Aborted");
			return;
		}
		
		//creates ComputeNodes
		for(InetSocketAddress p : participantLocations.keySet()){
			try {
				Socket client = new Socket(p.getAddress(), p.getPort());
				String message = "COMPUTE\n"+participantLocations.get(p)+"\n"+participantFileLocations.get(p)+"\n"+
								masterLocation.getHostName()+"\n"+masterNodePort+"\n"+masterDFSPort+"\n";
				PrintWriter out = new PrintWriter(client.getOutputStream());
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				out.println(message);
				out.flush();
				
				String response = in.readLine();
				client.close();
				if(response == null || response.equals("FAIL"))
					throw new IOException();
			} catch (IOException e) {

				numHosts--;
				participantLocations.remove(p);
				participants.remove(p);
				System.out.println("Participant " + p.toString() + " unavailable, will be removed from system");
			}
		}
		
		//Tell the masterDFS about the compute nodes
		try{
			Socket masterFS = new Socket(masterLocation.getAddress(), masterDFSPort);
			PrintWriter out = new PrintWriter(masterFS.getOutputStream());
			out.println("NODELOCATIONS");
			for(Map.Entry<InetSocketAddress,Integer> e : participantFileLocations.entrySet()){
				out.println(e.getKey().getHostName());
				out.println(e.getValue());
			}
			out.flush();
			out.close();
			masterFS.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		//Tell the master node about the configuration
		try
		{
			Socket client = new Socket(masterLocation.getAddress(),masterNodePort);
			
			
			OutputStream out = client.getOutputStream();
			ObjectOutput objOut = new ObjectOutputStream(out);
			
			//InputStream in = client.getInputStream();
			//ObjectInput objIn = new ObjectInputStream(in);
			Map<InetSocketAddress, Integer> dfsToMR=new TreeMap<InetSocketAddress,Integer>(new NodeCompare());
			for(Map.Entry<InetSocketAddress,Integer> e : participantFileLocations.entrySet()){
				dfsToMR.put(new InetSocketAddress(e.getKey().getHostName(),e.getValue()),participantLocations.get(e.getKey()));
			}
			MasterConfiguration cfg = new MasterConfiguration();
			cfg.dfsToMRports=dfsToMR;
			objOut.writeObject(cfg);

			client.close();
			
		} catch (IOException e) {
			System.out.println("Error: Unable to connect to Master");
			e.printStackTrace();
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

				String fname = f.getName();
				//for the j-th segment of the file
				for(int j = 0; j < fileSplit; j++){
					
					String startRecord = j*(numLines/fileSplit)+"%"; 
					String endRecord = (j+1)*(numLines/fileSplit)+"%";
					
					String fileID = startRecord+endRecord+fname;
					Socket[] fileRecipients = new Socket[replicationFactor];
					PrintWriter[] outStreams = new PrintWriter[replicationFactor];
					List<InetSocketAddress> fileCopies = new ArrayList<InetSocketAddress>();
					//for each replicating node
					String message = "NEWFILE\n"+fileID+"\n";
					for(int k = 0; k < replicationFactor; k++){
						
						InetSocketAddress p = participants.get((k+j) % numHosts);
						fileCopies.add(p);
						fileRecipients[k] = (new Socket(p.getHostName(), participantFileLocations.get(p)));
						outStreams[k] = new PrintWriter(fileRecipients[k].getOutputStream());	
						outStreams[k].println("FILESEND");	
						outStreams[k].println(fileID);
						message+=p.getHostName()+"\n";
						message+=participantFileLocations.get(p)+"\n";
					}
					//tell master who has this file
					Socket master = new Socket(masterLocation.getAddress(), masterDFSPort);
					PrintWriter outfs = new PrintWriter(master.getOutputStream());
					outfs.print(message);
					outfs.flush();
					outfs.close();
					master.close();
					
					//write file out to given nodes
					//make the last segment copy the rest of the file
					if(j != (fileSplit - 1)){
						for(int k = 0; k < numLines/fileSplit; k++){
							
							String line = readFile.readLine();
							//for each replicating node
							for(int l = 0; l < replicationFactor; l++){
								
								outStreams[l].println(line);
							}
						}
					}
					else{
						String line = "";
						while((line = readFile.readLine())!=null){
							
							//for each replicating node
							for(int l = 0; l < replicationFactor; l++){
								
								outStreams[l].println(line);
							}
						}
					}
					
					for(int k = 0; k < replicationFactor; k++){
						
						outStreams[k].close();
						fileRecipients[k].close();
					}
					
				}
				readFile.close();
				
			} catch (FileNotFoundException e1) {

				System.out.println("File " + f.getName() + " not found");
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}
	
	
}
