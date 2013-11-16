import java.io.Serializable;


public class Task implements Serializable {
	private static final long serialVersionUID = 1L;
	public int PID;
	public String file;
	public String mapReduceClass;
	public Type type;
	public enum Type {
		MAP,REDUCE 
	}
	
	public Task(int PID, Type t, String filename, String mapReduceClass){
		this.PID=PID;
		this.type=t;
		this.file=filename;
		this.mapReduceClass=mapReduceClass;
	}

}