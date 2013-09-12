import java.lang.reflect.*;
import java.util.*;
import java.net.*;
import java.io.*;


public class ProcessManager implements Runnable {

	private int port_num;
	private ServerSocket server;
	private List<MigratableProcess> processes;
	public static Object fileLock = new Object();
	
	public ProcessManager(int port_num){
		
		this.port_num = port_num;
		processes = new ArrayList<MigratableProcess>();
	}
	
	public MigratableProcess startProcess(String className, String[] args){
		
		Object instance = null;
		try {
			Class<?> newClass = Class.forName(className);
			Class<?>[] params = new Class[1];
			params[0] = Class.forName("[Ljava.lang.String;");
			Constructor<?> cons = newClass.getConstructor(params);
			Object[] params1 = new Object[1];
			params1[0] = args;
			instance = cons.newInstance(params1);
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
			return null;
		}
		else {
			newProcess=(MigratableProcess)instance;
		}
			
		//Start executing the process
		runProcess(newProcess);
		
		return newProcess;
	}
	
	private void runProcess(MigratableProcess p) {
		Thread processThread = new Thread(p);
		processes.add(p);
		processThread.start();
	}
	
	//Add functionality for suspend, query, etc
	
	public void migrateProcess(String newAddress, int port, MigratableProcess p){
		try{
			p.suspend();
			synchronized(ProcessManager.fileLock){
				Socket client = new Socket(newAddress,port);
				OutputStream out = client.getOutputStream();
				OutputStream buffer = new BufferedOutputStream(out);
				ObjectOutput output = new ObjectOutputStream(buffer);
				output.writeObject(p);
				output.close();
				client.close();
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void run()
	{
		 try{
			 server = new ServerSocket(port_num); 
		 } catch (IOException e) {
			 System.out.println("Could not listen on port " +Integer.toString(port_num));
			 e.printStackTrace();
			 //System.exit(-1);
		}
		 while(true) {
			 Socket client;
			 try{
				 client = server.accept();
				 InputStream in = client.getInputStream();
				 //Now we de-serialize the object
				 InputStream buffer= new BufferedInputStream(in);
				 ObjectInput obj = new ObjectInputStream(buffer);
				 MigratableProcess process = null;
				 process = (MigratableProcess) obj.readObject();
				 //Run the process on this machine
				 runProcess(process);
			 
			 } catch (ClassNotFoundException e) {
				 //Send response
			 } catch (IOException e) {
				 System.out.println("Accept failed");
				 System.exit(-1);
			 }
		 }
		 
	}
	
}
