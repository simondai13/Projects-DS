
public interface MapReducer {
	
	//User defined function to take a line from a record and output a single line
	public String map(String line);
	
	//User defined function to take an array of map outputs 
	public String[] reduce(String[] lines);

}
