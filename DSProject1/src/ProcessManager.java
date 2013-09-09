import java.lang.reflect.*;
import java.util.*;


public class ProcessManager {

	private List processes;
	
	public ProcessManager(){
		
		
	}
	
	public Object startProcess(String className, String[] args){
		
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
		
		if(!(instance instanceof MigratableProcess)){
			
			//Handle
		}
			
		
		return instance;
	}
	
	public void migrateProcess(){
		
	
	}
	
}
