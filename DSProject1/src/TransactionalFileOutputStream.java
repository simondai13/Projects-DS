import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;


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
	
	public TransactionalFileOutputStream(String filename, boolean what) {
		this.filename = filename;
		fileLocation = 0;
		openFile=null;
	}
	
	@Override
	public void write(int c) throws IOException {

		synchronized(ProcessManager.fileLock){
		
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
	
	private void writeObject(ObjectOutputStream out) throws IOException
	{
		openFile.close();
		openFile = null;
		out.defaultWriteObject();
	}

}
