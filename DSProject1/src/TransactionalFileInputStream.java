import java.io.*;


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

		synchronized(ProcessManager.fileLock){
		
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
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		openFile.close();
		openFile = null;
		out.defaultWriteObject();
	}

}
