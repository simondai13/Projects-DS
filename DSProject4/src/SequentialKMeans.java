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


public class SequentialKMeans {

	
	//args is of the form:
	//args[0] = number of clusters (k)
	//    [1] = d, R^d (number of dimensions)
	//    [2] = file containing points of the form (x1, x2, ..., xd), separated by a empty line
	//    [3] = output file
	public static void main(String [] args) throws IOException{
		
		//parsing
		int numClusters = Integer.parseInt(args[0]);
		int dimensions = Integer.parseInt(args[1]);
		File dataFile = new File(args[2]);
		File outFile = new File(args[3]);
		if(!outFile.exists())
			outFile.createNewFile();
		
		List<KTuple> points = new ArrayList<KTuple>();
		
		BufferedReader readData = new BufferedReader(new FileReader(dataFile));
		String line = "";
		while((line = readData.readLine()) != null){
			
			if(!line.equals("")){
				points.add(new KTuple(dimensions, line));
			}
		}
		readData.close();
		
		KMeans (numClusters, points, outFile);
	}
	
	
	
	public static void KMeans(int numClusters, List<KTuple> points, File out) throws FileNotFoundException{
		
		//initialize clusters
		
		//we will (arbitrarily) choose k points, by shuffling the list and taking the first k points.
		Collections.shuffle(points);
		List<KTuple> centroids = new ArrayList<KTuple>();
		for(int i = 0; i < numClusters; i++){
			
			centroids.add(points.get(i));
		}
		int mu = 0;
		boolean improvement = true;

		Map<KTuple, List<KTuple>> clusters = new TreeMap<KTuple, List<KTuple>>();
		
		//run algorithm
		while(improvement){
			improvement = false;
			++mu;
			
			//assign each point to the appropriate cluster
			for(KTuple p : points){

				double bestDistance = Double.MAX_VALUE;
				KTuple bestCentroid = null;
				for(KTuple centroid : centroids){
					
					double distance = euclideanDistance(p, centroid);
					if(distance < bestDistance){
						
						bestCentroid  = centroid;
					}
				}
				
				List<KTuple> tempList = null;
				if(!clusters.containsKey(bestCentroid))
					tempList = new ArrayList<KTuple>();
				
				else
					tempList = clusters.get(bestCentroid);
				tempList.add(p);
				clusters.put(bestCentroid, tempList);
			}
			
			System.out.println(clusters);
			//update centroids, see if improvement is big enough
			List<KTuple> newCentroids = new ArrayList<KTuple>();
			for(KTuple centroid : centroids){
				
				System.out.println(centroid);
				double[] newCentroidContent = new double[centroid.getK()];
				List<KTuple> clusterPoints = clusters.get(centroid);

				int numPoints = clusterPoints.size();
					
				for(int i = 0; i < centroid.getK(); i++){
					
					double sum = 0;
					for(KTuple p : clusterPoints){
						
						sum+=p.getValue(i);
					}
					newCentroidContent[i] = (sum/numPoints);
				}
				KTuple newCentroid = new KTuple(newCentroidContent);
				newCentroids.add(newCentroid);
				if(euclideanDistance(centroid, newCentroid) > 1)
					improvement = true;
			}
			centroids = newCentroids;
		}
		
		
		//write result to file
		PrintWriter fileOut = new PrintWriter(out);
		fileOut.println("K-Means iterations: " + mu + "\n");
		for(KTuple centroid : centroids){
			
			fileOut.println("Cluster Mean: " + centroid);
			fileOut.println("Cluster Points: " + clusters.get(centroid));
			fileOut.println();
		}
		fileOut.close();
	}
	
	public static double euclideanDistance(KTuple p1, KTuple p2){
		
		double sum = 0;
		int k = p1.getK();
		if(k != p2.getK())
			return -1;
		for(int i = 0; i < k; i ++){
			
			sum += Math.pow((p1.getValue(i) - p2.getValue(i)), 2);
		}
		
		return Math.sqrt(sum);
	}
	
	
}
