import java.io.*;


public class TransactionalFileInputStream extends InputStream implements
		Serializable {

	private String filename;
	private long fileLocation;
	Object lock;

	public TransactionalFileInputStream(String filename, Object lock) {
		
		this.filename = filename;
		fileLocation = 0;
		this.lock = lock;
	}

	public TransactionalFileInputStream(String filename) {
		
		this.filename = filename;
		fileLocation = 0;
	}
	
	@Override
	public int read() throws IOException {

		synchronized(lock){
		
			File fileToRead = new File(filename);
			RandomAccessFile stream = new RandomAccessFile(fileToRead, "r");
			stream.seek(fileLocation);
			
			int toReturn = stream.read();
			
			stream.close();
			fileLocation++;
			
			return toReturn;
		}
	}

}
