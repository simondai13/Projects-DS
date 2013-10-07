import java.io.Serializable;
import java.net.InetSocketAddress;


public class RegistryRequest implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public enum RequestType {
		BIND, LOOKUP, UNBIND
	}
	public RequestType type;
	public InetSocketAddress address;
	public String name;
}
