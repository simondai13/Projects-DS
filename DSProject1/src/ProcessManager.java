import java.util.TreeMap;


public class ProcessManager {
	//Structure to hold info on running process
	private class ProcessInfo{
		String url;
		int portNum;
		long processID;
		boolean isRunning;
		
		public ProcessInfo(String url, int portNum, long processID){
			isRunning=true;
			this.url=url;
			this.processID=processID;
			this.portNum=portNum;
		}
		
	}
	
	//List of processes maped to their ID's
	TreeMap processTable;
	
	public ProcessManager(){
		processTable = new TreeMap();
	}
	
	public long RunProcess(MigratableProcess p){return 0;}
	public boolean MigrateProcess(String destination){return true;}
	public boolean TerminateProcess(){return true;}
}
