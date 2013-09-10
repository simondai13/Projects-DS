import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;


//Example class that demonstrates process migration and file io
public class BadFileCopy implements MigratableProcess{

	private TransactionalFileInputStream  inFile;
	private TransactionalFileOutputStream outFile;
	private volatile boolean suspended;
	
	public BadFileCopy(String args[]){

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

		try {
			while (!suspended) {
				
				out.write(in.read());
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {}
			}
		} catch (EOFException e) {
			return;
		} catch (IOException e) {
			return;
		}

		suspended=false;
	}

	@Override
	public void suspend() {
		suspended=true;
		while(suspended);
	}
}
