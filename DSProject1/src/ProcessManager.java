import java.lang.reflect.*;
import java.util.*;


public class ProcessManager {

	public static int PORT_NUM = 5555;
	private List processes;
	
	public ProcessManager(){
		
		
	}
	
	public MigratableProcess startProcess(String className, String[] args){
		
		Object instance = null;
		try {
			Class<?> newClass = Class.forName(className);
			Constructor<?> cons = newClass.getConstructor();
			instance = cons.newInstance(args);
		} catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		} catch (NoSuchMethodException e) {

			e.printStackTrace();
		} catch (SecurityException e) {
			
			e.printStackTrace();
		} catch (InstantiationException e) {
			
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		}
		
		MigratableProcess newProcess=null;
		if(!(instance instanceof MigratableProcess)){
			
			//Handle
		}
		else {
			newProcess=(MigratableProcess)instance;
		}
			
		//Start executing the process
		Thread processThread = new Thread(newProcess);
		processThread.run();
		
		return newProcess;
	}
	
	public void migrateProcess(){
		
	
	}
	
	//The ability to specify a command-line port is added in the event 
	//that the default port PORT_NUM is unavailable.  However, it is the 
	//responsibility of the user to ensure that the port number is consistent
	//across nodes
	public static void main(String [] args)
	{
		int port=0;
		if(args.length > 0){
			try{
				port = Integer.parseInt(args[0]);
			}catch (NumberFormatException e) {
				System.out.println("Error, invalid port argument, using default: " + Integer.toString(PORT_NUM));
				port = PORT_NUM;
			}
		}
		PORT_NUM = port;
	}
	
}
