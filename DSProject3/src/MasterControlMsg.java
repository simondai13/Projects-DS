import java.io.Serializable;


public class MasterControlMsg implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public enum Type {
		START, PAUSE, RESUME, TERMINATE 
	}
	String[] files;
	Type type;
}
