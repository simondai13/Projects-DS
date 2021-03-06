import java.io.Serializable;
import java.net.InetSocketAddress;


public class StatusUpdate implements Serializable {
	private static final long serialVersionUID = 1L;
	public Task task;
	public enum Type {
		HEARTBEAT, TERMINATED, FAILED 
	}
	public InetSocketAddress node;
	public Type type;
	public StatusUpdate(Task t, Type type, InetSocketAddress node){
		task=t;
		this.type=type;
		this.node=node;
	}
}
