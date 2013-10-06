import java.io.*;

//Simple abstraction of a file input stream that reopen a file if the state of this input
//stream is serialized.  This allows file IO to work across multiple machines (Assuming a shared file system).
public class TransactionalFileInputStream extends InputStream implements
		Serializable {

	private String filename;
	private long fileLocation;
	private RandomAccessFile openFile;
	
	public TransactionalFileInputStream(String filename) {
		
		this.filename = filename;
		fileLocation = 0;
		openFile=null;
	}
	
	@Override
	public int read() throws FileNotFoundException, IOException {

		synchronized(ProcessManagerClient.fileLock){
		
			if(openFile == null)
				openStream();
			
			int toReturn = openFile.read();

			fileLocation++;
			
			return toReturn;
		}
	
	}
	
	@Override
	public void close() throws IOException {
		fileLocation = 0;
		openFile.close();
		super.close();
	}
	
	protected void openStream() throws FileNotFoundException, IOException
	{
		File fileToRead = new File(filename);
		openFile= new RandomAccessFile(fileToRead, "r");
		openFile.seek(fileLocation);
		
	}
	
	//This gets called when the object is serialized, close the open stream
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		if(openFile!=null)
			openFile.close();
		openFile = null;
		out.defaultWriteObject();
	}

}
