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
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mpi.*;

public class MPIKMeansDNA {
	public static int IMPROVEMENT_MARGIN = 0;

	// Constants indicating message content
	public static final int RESULTS = 1;
	public static final int COUNTS = 2;
	public static final int SUMS = 3;
	
	public static String outFilename;

	public static int dimensions;
	public static int numPoints;
	public static int partSize; // Number of points in a partion
	public static int lastPartSize; // Last partition has points lost to
									// rounding

	public static long startTime;
	public static int iters;

	public static void main(String[] args) throws IOException {

		// parsing
		int numClusters = Integer.parseInt(args[0]);
		dimensions = Integer.parseInt(args[1]);
		numPoints = Integer.parseInt(args[2]);
		int numParts = Integer.parseInt(args[3]);
		File dataFile = new File(args[4]);
		outFilename = args[5];

		partSize = numPoints / numParts;
		lastPartSize = numPoints - partSize * (numParts - 1);

		char[] strands = new char[numPoints * dimensions];
		int i = 0;

		BufferedReader readData = new BufferedReader(new FileReader(dataFile));
		String line = "";
		int idx = 0;
		while ((line = readData.readLine()) != null) {

			if (!line.equals("")) {
				System.arraycopy(line.toCharArray(), 0, strands, idx,
						dimensions);
				idx += dimensions;
			}
		}
		readData.close();
		try {
			MPIEval(args, strands, numClusters, dimensions, numParts);
		} catch (MPIException e) {
			e.printStackTrace();
		}

	}

	public static void MPIEval(String[] args, char[] allStrands,
			int numClusters, int dimensions, int numParts) throws MPIException {
		
		iters = 0;
		startTime = System.currentTimeMillis();
		
		MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();

		// This will be the master
		// The master is responsible for updating and broadcasting the k means
		if (rank == 0) {
			boolean[] improvement = new boolean[1];
			improvement[0] = true;
			char[] centers = new char[numClusters * dimensions];
			char[] strands = new char[partSize * dimensions];
			System.arraycopy(allStrands, 0, strands, 0, strands.length);
			int[] results = new int[partSize];

			//Randomly select some strands as the starting medians
			Set<Integer> startCenters = new TreeSet<Integer>();
			Random r = new Random();
			while (startCenters.size() < numClusters) {
				int i1 = r.nextInt(numPoints);
				if (!startCenters.contains(i1))
					startCenters.add(i1);
			}
			int idx = 0;
			for (Integer c : startCenters) {
				for (int i = 0; i < dimensions; i++) {
					centers[idx] = allStrands[c * dimensions + i];
					idx++;
				}
			}

			while (improvement[0]) {
				iters++;
				int[] centerCounts = new int[centers.length * 4];
				MPI.COMM_WORLD.Bcast(centers, 0, centers.length, MPI.CHAR, 0);
				// Do a fraction of the work on the master as well
				recomputeStrands(strands, centers, centerCounts, results);
				for (int i = 1; i < numParts; i++) {
					int[] tmpCounts = new int[centers.length * 4];
					MPI.COMM_WORLD.Recv(tmpCounts, 0, tmpCounts.length,
							MPI.INT, i, COUNTS);
					for (int j = 0; j < tmpCounts.length; j++) {
						centerCounts[j] += tmpCounts[j];
					}
				}

				int delta = 0;
				improvement[0] = false;
				for (int i = 0; i < centers.length; i++) {
					if (i % dimensions == 0) {
						if (delta > IMPROVEMENT_MARGIN) {
							improvement[0] = true;
						}
						delta = 0;
					}
					
					//Determine the most common letter at every index
					//and use that as the new mean
					
					int A = centerCounts[i * 4];
					int C = centerCounts[i * 4 + 1];
					int G = centerCounts[i * 4 + 2];
					int T = centerCounts[i * 4 + 3];

					char newIdx;
					if (A == C && A == G && A == T)
						newIdx = centers[i];
					else if (A >= C && A >= G && A >= T)
						newIdx = 'A';
					else if (C >= A && C >= G && C >= T)
						newIdx = 'C';
					else if (G >= A && G >= C && G >= T)
						newIdx = 'G';
					else
						newIdx = 'T';

					if (newIdx != centers[i])
						delta++;

					centers[i] = newIdx;
				}

				MPI.COMM_WORLD.Bcast(improvement, 0, 1, MPI.BOOLEAN, 0);
			}

			// We are done, aggregate the final cluster locations and points,
			// output the result
			List<List<String>> groupings = new ArrayList<List<String>>();
			List<String> kmeans = new ArrayList<String>();
			for (int i = 0; i < numClusters; i++) {
				char[] center = new char[dimensions];
				System.arraycopy(centers, i * dimensions, center, 0, dimensions);
				kmeans.add(String.copyValueOf(center));
				groupings.add(new ArrayList<String>());
			}

			// Process the results of the masters partition
			for (int i = 0; i < results.length; i++) {
				char[] strand = new char[dimensions];
				System.arraycopy(allStrands, i * dimensions, strand, 0,
						dimensions);
				groupings.get(results[i]).add(String.copyValueOf(strand));
			}

			// Now process the childs partitioning
			for (int i = 1; i < numParts; i++) {
				int[] res;
				if (i == numParts - 1) {
					res = new int[lastPartSize];
				} else {
					res = new int[partSize];
				}

				MPI.COMM_WORLD.Recv(res, 0, res.length, MPI.INT, i, RESULTS);
				for (int j = 0; j < res.length; j++) {
					char[] strand = new char[dimensions];
					System.arraycopy(allStrands, (partSize * i + j)
							* dimensions, strand, 0, dimensions);
					groupings.get(res[j]).add(String.copyValueOf(strand));
				}
			}

			writeResult(groupings, kmeans);
		}

		if (rank != 0) {
			boolean[] improvement = new boolean[1];
			improvement[0] = true;

			// Make sure we dont loose points to integer rounding
			char[] strands;
			if (rank == numParts - 1) {
				strands = new char[lastPartSize * dimensions];
			} else {
				strands = new char[partSize * dimensions];
			}
			System.arraycopy(allStrands, partSize * dimensions * rank, strands,
					0, strands.length);
			int[] results = new int[strands.length / dimensions];

			while (improvement[0]) {
				char[] centers = new char[numClusters * dimensions];
				int[] centerCounts = new int[centers.length * 4];
				MPI.COMM_WORLD.Bcast(centers, 0, centers.length, MPI.CHAR, 0);
				recomputeStrands(strands, centers, centerCounts, results);
				
				//Send the partial results from this partition
				MPI.COMM_WORLD.Send(centerCounts, 0, centerCounts.length,
						MPI.INT, 0, COUNTS);
				MPI.COMM_WORLD.Bcast(improvement, 0, 1, MPI.BOOLEAN, 0);
			}
			// Now send the point-cluster mapping. This is only done once we are
			// complete
			MPI.COMM_WORLD
					.Send(results, 0, results.length, MPI.INT, 0, RESULTS);
		}

		MPI.Finalize();
	}

	public static void recomputeStrands(char[] strands, char[] centers,
			int[] centerCounts, int[] results) {
		for (int i = 0; i < strands.length; i += dimensions) {
			int bestDistance = Integer.MAX_VALUE;
			int bestCentroid = 0;
			for (int j = 0; j < centers.length; j += dimensions) {

				// Compute the distance squared
				int distance2 = 0;
				for (int k = 0; k < dimensions; k++) {
					if (strands[i + k] != centers[j + k])
						distance2++;
				}
				// update the centroid
				if (distance2 < bestDistance) {
					bestDistance = distance2;
					bestCentroid = j;
					results[i / dimensions] = j / dimensions;
				}
			}
			//Count the letters at each index and tabulate the total results
			for (int k = 0; k < dimensions; k++) {
				switch (strands[i + k]) {
				case 'A':
				case 'a':
					centerCounts[(bestCentroid + k) * 4]++;
					break;
				case 'C':
				case 'c':
					centerCounts[(bestCentroid + k) * 4 + 1]++;
					break;
				case 'G':
				case 'g':
					centerCounts[(bestCentroid + k) * 4 + 2]++;
					break;
				case 'T':
				case 't':
					centerCounts[(bestCentroid + k) * 4 + 3]++;
					break;
				default:
					break;
				}

			}
		}
	}

	public static void writeResult(List<List<String>> groupings, 
									List<String> kmeans) {
		// write result to file
		long runtime = System.currentTimeMillis() - startTime;

		File outFile = new File(outFilename);
		try {
			if (!outFile.exists())
				outFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		PrintWriter fileOut = null;
		try {
			fileOut = new PrintWriter(outFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fileOut.println("K-Means iterations: " + iters + "\n");
		int i = 0;
		for (String centroid : kmeans) {

			fileOut.println("Cluster Mean: " + centroid);
			List<String> cluster = groupings.get(i);
			fileOut.println("Number of Cluster Points: " + cluster.size());
			fileOut.println("Cluster Points: " + cluster);
			fileOut.println();
			i++;
		}

		fileOut.println("Runtime (in milliseconds): " + runtime);
		fileOut.close();

		System.out.println("Runtime (in milliseconds): " + runtime);
	}

}
