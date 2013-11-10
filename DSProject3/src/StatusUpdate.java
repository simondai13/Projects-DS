import java.io.Serializable;


public class StatusUpdate implements Serializable {
	private static final long serialVersionUID = 1L;
	public long PID;
	public enum Type {
		HEARTBEAT, TERMINATED, FAILED 
	}
	public Type type;
}
