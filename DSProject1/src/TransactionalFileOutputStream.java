import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;


public class TransactionalFileOutputStream extends OutputStream implements
		Serializable {

	private String filename;
	private long fileLocation;

	public TransactionalFileOutputStream(String filename, boolean someBool){

		this.filename = filename;
		fileLocation = 0;
	}

	
	@Override
	public void write(int b) throws IOException {
		
		synchronized(ProcessManager.fileLock){
			
			File fileToRead = new File(filename);
			RandomAccessFile stream = new RandomAccessFile(fileToRead, "w");
			stream.seek(fileLocation);
			
			stream.write(b);
			
			stream.close();
			fileLocation++;
		}
	}

}
