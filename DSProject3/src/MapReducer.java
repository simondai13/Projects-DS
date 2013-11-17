import java.util.List;


public interface MapReducer {
	
	//User defined function to take a line from a record and output a single line
	public String map(String line);
	
	//User defined function to take an array of map outputs 
	public List<String> reduce(List<String> lines, String next);
	
	//User defined function to partition a record resulting from map
	public int partition(String s);
	
}
