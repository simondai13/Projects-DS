import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;


public class TransactionalFileOutputStream extends OutputStream implements
		Serializable {

	private String filename;
	private long fileLocation;
	Object lock;

	public TransactionalFileOutputStream(String filename, boolean someBool){
		
	}

	public TransactionalFileOutputStream(String filename, Object lock){
		
		this.filename = filename;
		this.fileLocation = 0;
		this.lock = lock;
	}
	
	
	@Override
	public void write(int b) throws IOException {
		
		synchronized(lock){
			
			File fileToRead = new File(filename);
			RandomAccessFile stream = new RandomAccessFile(fileToRead, "w");
			stream.seek(fileLocation);
			
			stream.write(b);
			
			stream.close();
			fileLocation++;
		}
	}

}
