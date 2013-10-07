import java.util.List;

public class RMIMessage {

	private String type; //Should be one of: INVOKE, RETURN, EXCEPTION
	
	//the arguments in the message
	//if INVOKE, then should be the arguments to be in
	//if RETURN, should be return value
	//if EXCEPTION, should be instance of the thrown Exception
	private Object arguments;
	
	public RMIMessage(String type, Object arguments){
		
		this.type = type;
		this.arguments = arguments;
	}
	
}
