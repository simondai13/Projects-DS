import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;




public class Master implements Runnable{
	 
	private TreeMap<Long,StatusUpdate.Type> jobs;
	private TreeMap<InetAddress,Boolean> nodeStatus;
	private long heartRate;
	private long delay;
	
	public Master(long heartbeatPeriod,long delay){
		heartRate=2*heartbeatPeriod;
		this.delay=delay;
	}
	
	private class WorkerCheck extends TimerTask {
		//Checks at a fixed interval if the running 
		@Override
		public void run() {
			synchronized(jobs) {
				for (Entry<InetAddress,Boolean> entry : nodeStatus.entrySet())
				{
					if(!entry.getValue())
					{
						handleFailure(entry.getKey());
					}
					entry.setValue(false);
					System.out.println("Lost connection to: " + entry.getKey().getHostAddress()+ "attempting recovery");
				}
			}
		}
	}
	
	private class ConnectionHandle implements Runnable {
		private Socket client;
		
		public ConnectionHandle(Socket client){
			this.client=client;
		}
		
		
		//Handle the life cycle of a remote object query, and then let this thread die
		@Override
		public void run() {
			try{ 
				 InputStream in = client.getInputStream();
				 ObjectInput objIn = new ObjectInputStream(in);
				 Object obj = objIn.readObject();
				 
				 //Make sure this message is packed properly
				 if(!StatusUpdate.class.isAssignableFrom(obj.getClass())){
					 return;
				 }
				 StatusUpdate stat = (StatusUpdate) obj;
				 synchronized (jobs)
				 {
					 switch(stat.type) {
						 case HEARTBEAT: 
					 		nodeStatus.put(client.getInetAddress(),true);
					 		break;
						 default:
							 jobs.put(stat.PID,stat.type);
					 }
				 }
				 
			 } catch (ClassNotFoundException e) {
				 System.out.println("Invalid Process Request");
			 }  catch (IOException e) {
				 System.out.println("Error receiving client message.");
			 }
		}
	}
		
	
	@Override
	//Simply accept connections and spawn ConnectionHandles as needed for this client
	public void run() {
		//Initialize the heartbeat monitor
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new WorkerCheck(),delay,heartRate);
		
		while(true) {
			try {
				Socket client = server.accept();
				
				//Generate a connection handle and run it in a 
				//separate thread
				ConnectionHandle ch = new ConnectionHandle(client);
				Thread t = new Thread(ch);
				t.start();
				
			} catch (IOException e) {
				
				System.out.println("CONNECTION FAILURE");
				// Just chalk it up to a failed connection and keep
				// running
			}
			 
				 
		}
				 
	}
}
