package rmi_framework;
import java.io.Serializable;

public class RMIMessage implements Serializable{
	private static final long serialVersionUID = 1L;

	public enum RMIMessageType{
		INVOKE, RETURN, EXCEPTION
	}
	
	private RMIMessageType type; //Should be one of: INVOKE, RETURN, EXCEPTION
	
	//the arguments in the message
	//if INVOKE, then should be a List of Objects the arguments to be used as the parameters
	//if RETURN, should be return value
	//if EXCEPTION, should be instance of the thrown Exception
	private Object[] arguments;
	
	private String ownerID;
	private String methodName;
	private Class<?>[] paramTypes;
	
	
	
	public RMIMessage(RMIMessageType type, Object[] arguments, String ownerID, String methodName, Class[] paramTypes){
		
		this.type = type;
		this.arguments = arguments;
		this.ownerID = ownerID;
		this.methodName = methodName;
		this.paramTypes = paramTypes;
	}
	
	public Object[] getArguments(){
		
		return arguments;
	}

	public RMIMessageType getMessageType(){
		
		return type;
	}
	
	public String getOwnerID(){
		
		return ownerID;
	}
	
	public String getMethodName(){
		
		return methodName;
	}
	
	public Class<?>[] getParamTypes(){
		
		return paramTypes;
	}
	
	
}
