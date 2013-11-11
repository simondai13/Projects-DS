import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//API for using files
//represents a single node in our distributed file system
//this class is not responsible for any location aware coordination across
//the distributed system
public class FileSystemNode {

	
	Map<String, File> localFiles;
	Map<String, File> tempFiles;
	
	public FileSystemNode(){
		
		localFiles = new TreeMap<String, File>();
		tempFiles = new TreeMap<String, File>();
	}
	
	//writes to end of file
	//file should EXIST before calling write
	public void write(String filename, String writeContent) throws FileNotFoundException{
		
		File f = localFiles.get(filename);
		File f2 = tempFiles.get(filename);
		if(f == null && f2 == null)
			throw new FileNotFoundException();
		
		if(f != null)
			write(f, writeContent, f.length());
		write(f2, writeContent, f2.length());
	}
	public void write(String filename, String writeContent, long writeLocation) throws FileNotFoundException{

		File f = localFiles.get(filename);
		File f2 = tempFiles.get(filename);
		if(f == null && f2 == null)
			throw new FileNotFoundException();
		
		if(f != null)
			write(f, writeContent, writeLocation);
		write(f2, writeContent, writeLocation);
	}
	private void write(File f, String writeContent, long writeLocation) throws FileNotFoundException{
		
		RandomAccessFile raf = new RandomAccessFile(f,"rw");
		try {
			raf.seek(writeLocation);
			raf.writeChars(writeContent);
			raf.close();
		} catch (IOException e) {
			
			// handle something here?
			e.printStackTrace();
		}
	}
	
	//returns if file was made successfully
	public boolean makeFile(String filename) throws IOException{
		
		File f = new File(filename);
		
		boolean result = f.createNewFile();
		if(result)
			localFiles.put(filename, f);
		
		return result;
	}
	public boolean makeTempFile(String filename) throws IOException{

		File f = new File(filename);
		
		boolean result = f.createNewFile();
		if(result)
			tempFiles.put(filename, f);
		
		return result;
	}
	//reads whole file
	public String read(String filename) throws FileNotFoundException{

		String result="";
		String line = "";
		File f = localFiles.get(filename);
		if(f == null)
			throw new FileNotFoundException();
		BufferedReader br = new BufferedReader(new FileReader(f));
		try {
			while((line = br.readLine()) != null){
				
				result += line;
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	public String read(String filename, long location, long readLength) throws FileNotFoundException{

		File f = localFiles.get(filename);
		if(f == null)
			throw new FileNotFoundException();
		return read(f, location, readLength);
	}
	private String read(File f, long location, long readLength) throws FileNotFoundException{

		RandomAccessFile raf = new RandomAccessFile(f,"rw");
		String result = "";
		try {
			raf.seek(location);
			for(int i = 0; i < readLength; ++i)
				result += raf.readChar();
			raf.close();
		} catch (IOException e) {
			
			// handle something here?
			e.printStackTrace();
		}
		return result;
	}
}
