import java.io.Serializable;
import java.net.InetSocketAddress;


public class FileLocationRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	public String filename;
	
	public enum Type {
		ADD, LOC
	}
	Type type;
	
	public InetSocketAddress[] nodes;
}
