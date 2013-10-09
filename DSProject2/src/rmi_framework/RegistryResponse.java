package rmi_framework;
import java.io.Serializable;
import java.net.InetSocketAddress;

public class RegistryResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public enum ResponseType {
		OK,NOT_FOUND,NAME_IN_USE
	}
	public ResponseType type;
	InetSocketAddress address;
	String name;
}
