import java.io.File;


//represents a partition of a given file
public class FilePartition {

	
	private File f;
	private long startInterval;
	private long endInterval;
	
	public FilePartition(File file, long s, long e){
		
		this.f = file;
		startInterval = s;
		endInterval = e;
	}
	
	public File getFile(){
		return f;
	}
	public long getStart(){
		return startInterval;
	}
	public long getEnd(){
		return endInterval;
	}
}
