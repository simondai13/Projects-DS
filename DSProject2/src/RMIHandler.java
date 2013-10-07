import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.List;


public class RMIHandler implements Runnable{

	private ServerSocket server;
	
	public RMIHandler(int port) throws IOException{
		
		server = new ServerSocket(port);
	}
	
	private class ConnectionHandler implements Runnable{

		private Socket client;
		
		public ConnectionHandler(Socket s){
			client = s;
		}
		
		public void dummyFunc(){System.out.println("s");}
		
		@Override
		public void run() {

			 OutputStream out;
			try {
				out = client.getOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(out);
				InputStream in = client.getInputStream();
				ObjectInput objIn = new ObjectInputStream(in);
				Object obj = objIn.readObject();

				if(!RMIMessage.class.isAssignableFrom(obj.getClass())){
					return;
				}
				RMIMessage msg = (RMIMessage) obj;
				
				if(msg.getMessageType().equals("INVOKE")){
					
					//invoke method with arguments.
					String methodName = msg.getMethodName();
					List<Object> arguments = (List<Object>)msg.getArguments();
					Object toReturn = null;
					
					try{
						
						
						
					}catch(Exception e){
						
						//write exception to client
						RMIMessage exceptionMessage = new RMIMessage("EXCEPTION", e, null, methodName);
						objOut.writeObject(exceptionMessage);
						return;
					}
					
					
					//return the return value
					RMIMessage returnMessage = new RMIMessage("RETURN", toReturn, null, methodName);
					objOut.writeObject(returnMessage);
				}
				else {System.out.println("BAD REQUEST"); return;}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	@Override
	public void run() {

		while(true){
			
			try {
				Socket clientConnection = server.accept();
				ConnectionHandler ch = new ConnectionHandler(clientConnection);
				Thread t = new Thread(ch);
				t.start();
			} catch (IOException e) {
				//ignore failed connections, continue to receive connections
			}
			
		}
	}

}
