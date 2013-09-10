
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
	
	public String toString(){
		
		return countdown+"\n";
	}
	
	@Override
	public void run() {

		while(!suspended && countdown > 0){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}

			countdown--;
			System.out.println(countdown);
		}
		
		if(countdown == 0)
			System.out.println("Boom");
		suspended = false;
	}

	@Override
	public void suspend() {
		suspended=true;
		while(suspended);
	}
}
