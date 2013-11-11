import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//primary manager class, to be used by application programmer
public class MapReduceManager {

	
	public void configMapReduce(File configFile){
		
		BufferedReader br = null;
		int maxmaps=1;
		int numPartitions=1;
		int replicationFactor=1;
		Map<InetSocketAddress, Integer> participantLocations = new TreeMap<InetSocketAddress, Integer>();
		InetSocketAddress masterLocation;
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
					participantLocations.put(new InetSocketAddress(address,  port),Integer.parseInt(line.substring(line.indexOf('=')+1)));
					break;
				case "MAXMAPS":
					maxmaps = Integer.parseInt(paramValue);
					break;
				case "MAXPARTITIONS":
					numPartitions = Integer.parseInt(paramValue);
					break;
				case "DATAFILE":
					String fname = paramValue;
					dataFiles.add(new File(fname));
					break;
				case "REPLICATION":
					replicationFactor = Integer.parseInt(paramValue);
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
	}
	
	
	public void bootstrap(){
		
	
	}
}
