import java.io.Serializable;
import java.util.List;


public class Task implements Serializable {
	private static final long serialVersionUID = 1L;
	public int PID;
	public List<String> files;
	public String mapReduceClass;
	public Type type;
	public enum Type {
		MAP,REDUCE 
	}
	
	public Task(int PID, Type t, List<String> files, String mapReduceClass){
		this.PID=PID;
		this.type=t;
		this.files=files;
		this.mapReduceClass=mapReduceClass;
	}

}