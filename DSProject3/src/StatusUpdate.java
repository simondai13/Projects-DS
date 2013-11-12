import java.io.Serializable;
import java.net.InetSocketAddress;


public class StatusUpdate implements Serializable {
	private static final long serialVersionUID = 1L;
	public long PID;
	public enum Type {
		HEARTBEAT, TERMINATED, FAILED 
	}
	public InetSocketAddress node;
	public Type type;
}
