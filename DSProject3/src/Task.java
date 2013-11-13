import java.io.Serializable;


public class Task implements Serializable {
	private static final long serialVersionUID = 1L;
	public int PID;
	public String file;
	public Type type;
	public enum Type {
		MAP,REDUCE 
	}
	
	public Task(int PID, Type t, String filename){
		this.PID=PID;
		this.type=t;
		this.file=filename;
	}

}