package rmi_framework;
import java.io.Serializable;

//Simple message structure for replying to registry requests
public class RegistryResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public enum ResponseType {
		OK,NOT_FOUND,NAME_IN_USE
	}
	public ResponseType type;
	public RemoteObjectRef obj;
}
