import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;


//Example class that demonstrates process migration and file io
public class TimeBombFile implements MigratableProcess{

	private int countdown;
	private TransactionalFileOutputStream outFile;
	private volatile boolean suspended;
	
	public TimeBombFile(String args[]){

		if (args.length != 2) {
			System.out.println("arguments: <inputFile> <outputFile>");
			throw new IllegalArgumentException("Invalid Arguments");
		}
		
		countdown = Integer.parseInt(args[0]);
		outFile = new TransactionalFileOutputStream(args[1], false);
		suspended = false;
	}

	@Override
	public void run() {

		PrintStream out = new PrintStream(outFile);
		
		while((!suspended) && countdown > 0){

			countdown--;
			out.println(countdown);
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			
		}
		
		if(countdown == 0){
			out.println("Boom");
			try{
				outFile.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		suspended=false;
	}

	@Override
	public void suspend() {
		suspended=true;
		
		try {
			outFile.write(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while(suspended);
	}
}
