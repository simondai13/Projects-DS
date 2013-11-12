import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;


public class Scheduler {
	private TreeMap<String,InetSocketAddress[]> fileLocs;
	private String[] mapFiles;
	private InetSocketAddress[] workNodes;
    private TreeMap<Task,InetSocketAddress> initialTasks;
    private List<Task> mapTasks;
    private List<Task> reduceTasks;
	
	private int PID_Index;
	public Scheduler(String[] mapFiles, InetSocketAddress[] workNodes, TreeMap<String,InetSocketAddress[]> fileLocs){
		PID_Index=0;
		this.mapFiles=mapFiles;
		this.workNodes=workNodes;
		this.fileLocs=fileLocs;
		mapTasks = new ArrayList<Task>();
		reduceTasks = new ArrayList<Task>();
		initialTasks = new TreeMap<Task,InetSocketAddress>();
		//setup an initial schedule
		bipartiteMatch();
		System.out.println(initialTasks.toString());
	} 
	
	public TreeMap<Task,InetSocketAddress> getInitialTasks(){
		return initialTasks;
	}
	
	public Task getNextTask(InetSocketAddress node){
		if(!mapTasks.isEmpty()){
			for(int i=0; i<mapTasks.size(); i++){
				InetSocketAddress[] locs =fileLocs.get(mapTasks.get(i));
				for(int j=0; i<locs.length; j++){
					if(node.equals(locs[i])){
						Task result= mapTasks.get(i);
						mapTasks.remove(i);
						return result;
						
					}
				}
			}
		
			Task result= mapTasks.get(0);
			mapTasks.remove(0);
			return result;
		}
		else if(!reduceTasks.isEmpty()){
			Task result= reduceTasks.get(0);
			reduceTasks.remove(0);
			return result;
		}
		
		return null;
	}
	
	public void addTask(Task t){
		/*if()
		{
			reduceTasks.add(t);
		}
		*/
	}
	
	
	//This scheduler uses an adaptation of the augmenting paths algorithm.
	//The scheduler will attempt to use the AUGMENTING PATHSs algorithm to evenly
	//distribute the mapping operations on top of an instance of the data getting
	//mapped.  After this, any remaining mapping operations are simply assigned to
	//the first free worker node
	private void bipartiteMatch()
	{	
		//Create an adjacency matrix from file locations, note it is not square because
		//we need only consider edges in a single direction
		boolean[][] adjMatrix = new boolean[mapFiles.length][workNodes.length];
		for (int i=0; i<mapFiles.length; i++) {
			for(int j=0; j<workNodes.length; j++){
				adjMatrix[i][j] = isEdge(mapFiles[i],workNodes[j]);
			}
		}
		
		////Begin with M as the empty path
		int[] M= new int[workNodes.length];
	    for(int i=0; i<M.length; i++){
	    	M[i]=-1;
	    }
	 
	    for (int i = 0; i < fileLocs.size(); i++)
	    {
	        boolean[] visited=new boolean[workNodes.length];
	        augPath(adjMatrix,M,visited,i);
	    }
	    
	    boolean[] taskedMaps = new boolean[mapFiles.length];

	    for(int i=0; i<M.length; i++){
	    	if(M[i]!=-1){
	    		initialTasks.put(new Task(PID_Index,Task.Type.MAP,mapFiles[M[i]]), workNodes[i]);
	    		PID_Index++;
	    		taskedMaps[M[i]]=true;
	    	}
	    }
	    
	    //Any map tasks that did not get an optimal assignment
	    //are placed in the work queue and assigned to the first available node
	    for(int i=0; i<taskedMaps.length; i++)
	    {
	    	if(!taskedMaps[i]){
	    		mapTasks.add(new Task(PID_Index,Task.Type.MAP,mapFiles[i]));
	    		PID_Index++;
	    	}
	    }
	    
	}


	//Search for an augmenting path M' using DFS, we start with an unmatched node and
	//try each possible matching of that node to see if it is indeed an augmenting path
	//returns false if there is no augmented path or true if there is, with M set to M'
	//(the augmented path)
	// A DFS based recursive function that returns true if a
	// matching for vertex u is possible
	private boolean augPath(boolean[][] adjMatrix, int[] M, boolean[] visited, int v)
	{
	    for (int i = 0; i < adjMatrix[0].length; i++)
	    {
	        // If applicant u is interested in job v and v is
	        // not visited
	        if (!visited[i] && adjMatrix[v][i])
	        {
	            visited[i] = true; // Mark v as visited
	 
	            if (M[i] == -1 || augPath(adjMatrix,M, visited, M[i]))
	            {
	                M[i] = v;
	                return true;
	            }
	        }
	    }
	    return false;
	}
	
	//Helper function to find if there is an edge between 
	//file and address (ie the file is at the address)
	private boolean isEdge(String file, InetSocketAddress addr){
		InetSocketAddress[] locs=fileLocs.get(file);
		if(locs == null)
			return false;
		
		for(int i=0; i<locs.length; i++){
			if(locs[i].equals(addr))
				return true;
		}
		
		return false;
	}
	
	
	
}