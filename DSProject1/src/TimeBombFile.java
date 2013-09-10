import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;


//Example class that demonstrates process migration and file io
public class TimeBombFile implements MigratableProcess{

	private TransactionalFileInputStream  inFile;
	private TransactionalFileOutputStream outFile;
	private volatile boolean suspended;
	
	public TimeBombFile(String args[]){

		if (args.length != 2) {
			System.out.println("arguments: <inputFile> <outputFile>");
			throw new IllegalArgumentException("Invalid Arguments");
		}
		inFile = new TransactionalFileInputStream(args[0]);
		outFile = new TransactionalFileOutputStream(args[1], false);
		suspended = false;
	}

	@Override
	public void run() {

		PrintStream out = new PrintStream(outFile);
		DataInputStream in = new DataInputStream(inFile);
		int countdown = 0;
		
		try {
			countdown = in.readInt();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while((!suspended) && countdown > 0){

			countdown--;
			out.println(countdown);
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			
		}
		
		if(countdown == 0)
			out.println("Boom");
		
		suspended=false;
	}

	@Override
	public void suspend() {
		suspended=true;
		while(suspended);
	}
}
