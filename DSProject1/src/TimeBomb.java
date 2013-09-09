
//Example class that demonstrates process migration.
//This class does NOT use any file IO
public class TimeBomb implements MigratableProcess{

	private int countdown;
	private volatile boolean suspended;
	
	public TimeBomb(String args[]){
		
		if(args.length != 1)
			throw new IllegalArgumentException("");
		countdown = Integer.parseInt(args[0]);
		suspended = false;
	}
	
	public void decrementTime(){
		
		countdown--;
	}

	@Override
	public void run() {

		while(!suspended && countdown > 0){
			
			countdown--;
		}
		
	}

	@Override
	public void suspend() {
		suspended=true;
	}
}
