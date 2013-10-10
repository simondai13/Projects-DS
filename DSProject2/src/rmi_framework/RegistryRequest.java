package rmi_framework;
import java.io.Serializable;

//Message type used for sending register, lookup, and unregister requests to the registry
//server
public class RegistryRequest implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public enum RequestType {
		REGISTER, LOOKUP, UNREGISTER
	}
	public RequestType type;
	public RemoteObjectRef obj;
	public String name;
}
