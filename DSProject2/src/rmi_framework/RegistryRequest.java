package rmi_framework;
import java.io.Serializable;
import java.net.InetSocketAddress;


public class RegistryRequest implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public enum RequestType {
		REGISTER, LOOKUP, UNREGISTER
	}
	public RequestType type;
	public RemoteObjectRef obj;
	public String name;
}
