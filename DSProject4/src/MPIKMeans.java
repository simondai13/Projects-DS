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

public class MPIKMeans {
	public static double IMPROVEMENT_MARGIN = .00001;

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
	public static int iters;
	public static long startTime;

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

		double[] points = new double[numPoints * dimensions];
		int i = 0;

		BufferedReader readData = new BufferedReader(new FileReader(dataFile));
		String line = "";
		while ((line = readData.readLine()) != null) {
			if (!line.equals("")) {
				String[] split = (line.replaceAll("[()]", "")).split(",");
				for (int j = 0; j < dimensions; j++) {
					String num = split[j].trim();
					points[i] = Double.parseDouble(num);
					i++;
				}
			}
		}
		readData.close();
		try {
			MPIEval(args, points, numClusters, dimensions, numParts);
		} catch (MPIException e) {
			e.printStackTrace();
		}

	}

	public static void MPIEval(String[] args, double[] allPoints,
			int numClusters, int dimensions, int numParts) throws MPIException {
		
		MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();

		iters = 0;
		startTime = System.currentTimeMillis();

		// This will be the master
		// The master is responsible for updating and broadcasting the k means
		if (rank == 0) {
			boolean[] improvement = new boolean[1];
			improvement[0] = true;
			double[] centers = new double[numClusters * dimensions];
			double[] points = new double[partSize * dimensions];
			System.arraycopy(allPoints, 0, points, 0, points.length);
			int[] results = new int[partSize];

			Set<Integer> startCenters = new TreeSet<Integer>();
			Random r = new Random();
			while (startCenters.size() < numClusters) {
				int i1 = r.nextInt(numPoints);
				startCenters.add(i1);
			}
			int idx = 0;
			for (Integer c : startCenters) {
				for (int i = 0; i < dimensions; i++) {
					centers[idx] = allPoints[c * dimensions + i];
					idx++;
				}
			}

			while (improvement[0]) {
				iters++;
				double[] centerValues = new double[numClusters * dimensions];
				int[] centerCounts = new int[numClusters];
				MPI.COMM_WORLD.Bcast(centers, 0, centers.length, MPI.DOUBLE, 0);
				// Do a fraction of the work on the master as well
				recomputeCenters(points, centers, centerCounts, centerValues,
						results);
				for (int i = 1; i < numParts; i++) {
					double[] tmpCenters = new double[numClusters * dimensions];
					int[] tmpCounts = new int[numClusters];
					MPI.COMM_WORLD.Recv(tmpCenters, 0, tmpCenters.length,
							MPI.DOUBLE, i, SUMS);
					MPI.COMM_WORLD.Recv(tmpCounts, 0, tmpCounts.length,
							MPI.INT, i, COUNTS);
					for (int j = 0; j < tmpCounts.length; j++) {
						centerCounts[j] += tmpCounts[j];
						for (int k = 0; k < dimensions; k++) {
							centerValues[j * dimensions + k] += tmpCenters[j
									* dimensions + k];
						}
					}
				}

				double delta = 0;
				improvement[0] = false;
				for (int i = 0; i < centers.length; i++) {
					if (i % dimensions == 0) {
						if (delta > IMPROVEMENT_MARGIN) {
							improvement[0] = true;
						}
						delta = 0;
					}
					double newVal = centerValues[i]
							/ centerCounts[i / dimensions];
					delta += (centers[i] - newVal) * (centers[i] - newVal);
					centers[i] = newVal;

				}
				// This was used to produce meaningful results, there was too
				// much variation in
				// # of iterations to see parallelism effect without it
				// if(iters>=30)
				// improvement[0]=false;

				MPI.COMM_WORLD.Bcast(improvement, 0, 1, MPI.BOOLEAN, 0);
			}

			// We are done, aggregate the final cluster locations and points,
			// output the result
			List<List<KTuple>> groupings = new ArrayList<List<KTuple>>();
			List<KTuple> kmeans = new ArrayList<KTuple>();
			for (int i = 0; i < numClusters; i++) {
				double[] center = new double[dimensions];
				System.arraycopy(centers, i * dimensions, center, 0, dimensions);
				kmeans.add(new KTuple(center));
				groupings.add(new ArrayList<KTuple>());
			}

			// Process the results of the masters partition
			for (int i = 0; i < results.length; i++) {
				double[] point = new double[dimensions];
				System.arraycopy(allPoints, i * dimensions, point, 0,
						dimensions);
				groupings.get(results[i]).add(new KTuple(point));
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
					double[] point = new double[dimensions];
					System.arraycopy(allPoints,
							(partSize * i + j) * dimensions, point, 0,
							dimensions);
					
					groupings.get(res[j]).add(new KTuple(point));
				}
			}

			writeResult(groupings, kmeans);
		}

		if (rank != 0) {
			boolean[] improvement = new boolean[1];
			improvement[0] = true;

			// Make sure we dont loose points to integer rounding
			double[] points;
			if (rank == numParts - 1) {
				points = new double[lastPartSize * dimensions];
			} else {
				points = new double[partSize * dimensions];
			}
			System.arraycopy(allPoints, partSize * dimensions * rank, points,
					0, points.length);
			int[] results = new int[points.length / dimensions];

			while (improvement[0]) {
				double[] centers = new double[numClusters * dimensions];
				double[] centerValues = new double[numClusters * dimensions];
				int[] centerCounts = new int[numClusters];
				MPI.COMM_WORLD.Bcast(centers, 0, centers.length, MPI.DOUBLE, 0);
				recomputeCenters(points, centers, centerCounts, centerValues,
						results);

				//Send the results of this nodes work for this iteration
				MPI.COMM_WORLD.Send(centerValues, 0, centers.length,
						MPI.DOUBLE, 0, SUMS);
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

	//This function computes the sum of all points for each center, as well
	//as a count of the number of points for each center.  We can then combine
	//partitions to find a new mean for each center
	public static void recomputeCenters(double[] points, double[] centers,
			int[] centerCounts, double[] centerValues, int[] results) {
		for (int i = 0; i < points.length; i += dimensions) {
			double bestDistance = Double.MAX_VALUE;
			int bestCentroid = 0;
			for (int j = 0; j < centers.length; j += dimensions) {

				// Compute the distance squared
				double distance2 = 0;
				for (int k = 0; k < dimensions; k++) {
					distance2 += (points[i + k] - centers[j + k])
							* (points[i + k] - centers[j + k]);
				}
				// update the centroid
				if (distance2 < bestDistance) {
					bestDistance = distance2;
					bestCentroid = j;
					results[i / dimensions] = j / dimensions;
				}
			}
			centerCounts[bestCentroid / dimensions]++;
			for (int k = 0; k < dimensions; k++) {
				centerValues[bestCentroid + k] += points[i + k];
			}
		}
	}

	//Write the output of this kmeans algorithm
	public static void writeResult(List<List<KTuple>> groupings, 
									List<KTuple> kmeans) {

		// Stop the timer now to prevent file io being counted
		long runtime = System.currentTimeMillis() - startTime;

		// write result to file
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
		for (KTuple centroid : kmeans) {

			fileOut.println("Cluster Mean: " + centroid);
			List<KTuple> cluster = groupings.get(i);
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
