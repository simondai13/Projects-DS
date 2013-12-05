import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class SequentialKMeansDNA {

	public static int IMPROVEMENT_MARGIN = 0; 
	
	//args is of the form:
	//args[0] = number of clusters (k)
	//    [1] = file containing DNA strands as strings of the form AGTTATAG...
	//    [2] = output file
	public static void main(String [] args) throws IOException{
		
		//parsing
		int numClusters = Integer.parseInt(args[0]);
		File dataFile = new File(args[1]);
		File outFile = new File(args[2]);
		if(!outFile.exists())
			outFile.createNewFile();
		
		List<String> sequences = new ArrayList<String>();
		
		BufferedReader readData = new BufferedReader(new FileReader(dataFile));
		String line = "";
		while((line = readData.readLine()) != null){
			
			if(!line.equals("")){
				sequences.add(line);
			}
		}
		readData.close();
		
		KMeans (numClusters, sequences, outFile);
	}
	
	
	
	public static void KMeans(int numClusters, List<String> sequences, File out) throws FileNotFoundException{
		
		long startTime = System.currentTimeMillis();
		//initialize clusters
		
		//we will (arbitrarily) choose k points, by shuffling the list and taking the first k points.
		Collections.shuffle(sequences);
		List<String> centroids = new ArrayList<String>();
		for(int i = 0; i < numClusters; i++){
			
			centroids.add(sequences.get(i));
		}
		
		//System.out.println("INIT: " + centroids); 
		int mu = 0;
		boolean improvement = true;

		Map<String, List<String>> clusters = null;
		
		//run algorithm
		while(improvement){
			improvement = false;
			++mu;
			clusters = new TreeMap<String, List<String>>();
			//assign each point to the appropriate cluster
			for(String s : sequences){

				int bestDistance = Integer.MAX_VALUE;
				String bestCentroid = null;
				for(String centroid : centroids){
					
					int distance = dnaDistance(s, centroid);
					if(distance < bestDistance){
						
						bestDistance = distance;
						bestCentroid  = centroid;
					}
				}
				
				List<String> tempList = null;
				if(!clusters.containsKey(bestCentroid))
					tempList = new ArrayList<String>();
				
				else
					tempList = clusters.get(bestCentroid);
				tempList.add(s);
				//System.out.println("Best Centroid: " + bestCentroid + " for point " + p + " with distance " + bestDistance);
				clusters.put(bestCentroid, tempList);
			}
			
			//System.out.println("Cluster Centers: " + clusters.keySet());
			
			//update centroids, see if improvement is big enough

			Map<String, List<String>> newClusters = new TreeMap<String, List<String>>();
			List<String> newCentroids = new ArrayList<String>();
			
			for(String centroid : centroids){
				
				//System.out.println(centroid);

				List<String> clusterSequences = clusters.get(centroid);

				int numSequences = clusterSequences.size();
					
				String newCentroid = "";
				
				for(int i = 0; i < centroid.length(); i++){
					
					int aCount=0, gCount=0, tCount=0, cCount=0;;
					for(String s : clusterSequences){
						
						char letter = s.charAt(i);
						if(letter == 'A' || letter == 'a')
							aCount++;
						else if(letter == 'C' || letter == 'c')
							cCount++;
						else if(letter == 'G' || letter == 'g')
							gCount++;
						else if(letter == 'T' || letter == 't')
							tCount++;
					}

					if(aCount >= cCount && aCount >= gCount && aCount >= tCount)
						newCentroid += 'A';
					else if(cCount >= aCount && cCount >= gCount && cCount >= tCount)
						newCentroid += 'C';
					else if(gCount >= cCount && gCount >= aCount && gCount >= tCount)
						newCentroid += 'G';
					else
						newCentroid += 'T';
				}
				newCentroids.add(newCentroid);
				newClusters.put(newCentroid, clusters.get(centroid));
				
				if(dnaDistance(centroid, newCentroid) > IMPROVEMENT_MARGIN)
					improvement = true;
			}
			centroids = newCentroids;
			clusters = newClusters;
		}
		
		
		//write result to file
		PrintWriter fileOut = new PrintWriter(out);
		fileOut.println("K-Means iterations: " + mu + "\n");
		for(String centroid : centroids){

			fileOut.println("Cluster Mean: " + centroid);
			List<String> cluster = clusters.get(centroid);
			fileOut.println("Number of Cluster Points: " + cluster.size());
			fileOut.println("Cluster Points: " + cluster);
			fileOut.println();
		}

		long runtime = System.currentTimeMillis() - startTime;
		
		fileOut.println("Runtime (in milliseconds): " + runtime);
		fileOut.close();
		
		System.out.println("Runtime (in milliseconds): " + runtime);
	}
	
	
	public static int dnaDistance(String a, String b){
		
		int length = Math.min(a.length(), b.length());
		int diff = 0;
		for(int i =0; i < length; i++){
			
			if(a.charAt(i) != b.charAt(i))
				diff++;
		}
		
		return diff;
		
		
	}
}
