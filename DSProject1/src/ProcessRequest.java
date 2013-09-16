import java.io.Serializable;

//This class is a means of packaging up a request to the processmanager server.
//The request is stored in the req field, and the response is stored in the resp field, the destination has
//various interpretations based on the type of request and the GUID represents the process in question.

public class ProcessRequest implements Serializable {
	public RequestType req;
	public ResponseType resp;
	public NodeAddr destination;
	public long guid;
	
}
