import java.io.Serializable;


public class ProcessRequest implements Serializable {
	public RequestType req;
	public ResponseType resp;
	public NodeAddr destination;
	public long guid;
	
}
