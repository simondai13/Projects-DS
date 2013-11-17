import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class Scheduler {
	private TreeMap<String,List<InetSocketAddress>> fileLocs;
	private String[] mapFiles;
	private List<InetSocketAddress> workNodes;
    private TreeMap<InetSocketAddress,List<Task>> initialTasks;
    private List<Task> mapTasks;
    private List<Task> reduceTasks;
    private String mapReducer;
    private int numCores;
    private int numPartitions;
    public boolean initialMapsComplete;
	
	private int PID_Index;
	public Scheduler(List<InetSocketAddress> workNodes, TreeMap<String,List<InetSocketAddress>> fileLocs, String mapReducer,int numCores,int numParts){
		PID_Index=0;
		mapFiles=new String[fileLocs.size()];
		int i=0;
		for(Map.Entry<String,List<InetSocketAddress>> e :fileLocs.entrySet()){
			mapFiles[i]=e.getKey();
			i++;
		}
		System.out.println(mapFiles.length);
		this.workNodes=workNodes;
		this.fileLocs=fileLocs;
		this.mapReducer=mapReducer;
		this.numCores=numCores;
		this.numPartitions=numParts;
		mapTasks = new ArrayList<Task>();
		reduceTasks = new ArrayList<Task>();
		initialTasks = new TreeMap<InetSocketAddress,List<Task>>(new NodeCompare());
		for(InetSocketAddress addr: workNodes){
			initialTasks.put(addr, new ArrayList<Task>());
		}
		initialMapsComplete=false;

		//setup an initial schedule
		synchronized(fileLocs){
			bipartiteMatch();
		}
		//Simply add a reduce task for each file
		for(i=0; i<mapFiles.length; i++){
			reduceTasks.add(new Task(PID_Index,Task.Type.REDUCE,mapFiles[i],mapReducer));
			PID_Index++;
		}
		
	} 
	
	public TreeMap<InetSocketAddress,List<Task>> getInitialTasks(){
		return initialTasks;
	}
	
	public Task getNextTask(InetSocketAddress node){
		if(!mapTasks.isEmpty()){
			synchronized(fileLocs){
				for(int i=0; i<mapTasks.size(); i++){
					List<InetSocketAddress> locs =fileLocs.get(mapTasks.get(i).files.get(0));
					for(int j=0; i<locs.size(); j++){
						if(node.equals(locs.get(i))){
							Task result= mapTasks.get(i);
							mapTasks.remove(i);
							return result;
							
						}
					}
				}
			}
			Task result= mapTasks.get(0);
			mapTasks.remove(0);
			return result;
		}
		else if(!reduceTasks.isEmpty() && initialMapsComplete){
			Task result= reduceTasks.get(0);
			reduceTasks.remove(0);
			return result;
		}
		
		return null;
	}
	
	
	public void addTask(Task t){
		if(t.type==Task.Type.REDUCE)
		{
			reduceTasks.add(t);
		}else
		{
			mapTasks.add(t);
		}
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
		boolean[][] adjMatrix = new boolean[mapFiles.length][workNodes.size()*numCores];
		for (int i=0; i<mapFiles.length; i++) {
			for(int j=0; j<workNodes.size()*numCores; j++){
				adjMatrix[i][j] = isEdge(mapFiles[i],workNodes.get(j/numCores));
			}
		}
		
		////Begin with M as the empty path
		int[] M= new int[workNodes.size()*numCores];
	    for(int i=0; i<M.length; i++){
	    	M[i]=-1;
	    }
	 
	    for (int i = 0; i < mapFiles.length; i++)
	    {
	        boolean[] visited=new boolean[workNodes.size()*numCores];
	        augPath(adjMatrix,M,visited,i);
	    }
	    
	    boolean[] taskedMaps = new boolean[mapFiles.length];

	    for(int i=0; i<M.length; i++){
	    	if(M[i]!=-1){
	    		System.out.println(initialTasks);
	    		List<Task> tasks= initialTasks.get(workNodes.get(i/numCores));
	    		List<String> fs= new ArrayList<String>();
	    		fs.add(mapFiles[M[i]]);
	    		tasks.add(new Task(PID_Index,Task.Type.MAP,fs,mapReducer));
	    		PID_Index++;
	    		taskedMaps[M[i]]=true;
	    	}
	    }
	    
	    //Any map tasks that did not get an optimal assignment
	    //are placed in the work queue and assigned to the first available node
	    for(int i=0; i<taskedMaps.length; i++)
	    {
	    	if(!taskedMaps[i]){
	    		List<String> fs= new ArrayList<String>();
	    		fs.add(mapFiles[i]);
	    		mapTasks.add(new Task(PID_Index,Task.Type.MAP,fs,mapReducer));
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
		System.out.println(addr);
		List<InetSocketAddress> locs=fileLocs.get(file);
		if(locs == null)
			return false;
		System.out.println(fileLocs.get(file));
		
		for(int i=0; i<locs.size(); i++){
			if(locs.get(i).equals(addr))
				return true;
		}
		
		return false;
	}
	
	
	
}