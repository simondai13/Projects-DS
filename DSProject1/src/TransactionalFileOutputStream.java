import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

//Simple abstraction of a file input stream that reopen a file if the state of this input
//stream is serialized.  This allows file IO to work across multiple machines (Assuming a shared file system).
public class TransactionalFileOutputStream extends OutputStream implements
		Serializable {

	private String filename;
	private long fileLocation;
	private RandomAccessFile openFile;
	
	public TransactionalFileOutputStream(String filename) {
		
		this.filename = filename;
		fileLocation = 0;
		openFile=null;
	}
	
	
	@Override
	public void write(int c) throws FileNotFoundException, IOException {

		synchronized(ProcessManagerClient.fileLock){
		
			if(openFile == null)
				openStream();
			
			openFile.write(c);

			fileLocation++;
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
		File fileToWrite = new File(filename);
		openFile= new RandomAccessFile(fileToWrite, "rw");
		openFile.seek(fileLocation);
		
	}
	
	//Called when the stream is serialized, close the open file
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		if(openFile!=null)
			openFile.close();
		openFile = null;
		out.defaultWriteObject();
	}

}
