import java.io.Serializable;

public class RMIMessage implements Serializable{

	private String type; //Should be one of: INVOKE, RETURN, EXCEPTION
	
	//the arguments in the message
	//if INVOKE, then should be a List of Objects the arguments to be used as the parameters
	//if RETURN, should be return value
	//if EXCEPTION, should be instance of the thrown Exception
	private Object arguments;
	
	private Object reference;
	private String methodName;
	
	public RMIMessage(String type, Object arguments, Object reference, String methodName){
		
		this.type = type;
		this.arguments = arguments;
	}
	
	public Object getArguments(){
		
		return arguments;
	}

	public String getMessageType(){
		
		return type;
	}
	
	public Object getReference(){
		
		return reference;
	}
	
	public String getMethodName(){
		
		return methodName;
	}
	
	
}
