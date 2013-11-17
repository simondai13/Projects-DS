<<<<<<< HEAD

public class Histogram implements MapReducer {
	
	@Override
	public String map(String line) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}

	@Override
	public String[] reduce(String[] lines) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lines;
	}

}
=======
public class Histogram implements MapReducer {

	@Override
	public String map(String line) {
		return line;
	}

	@Override
	public String[] reduce(String[] lines) {
		return lines;
	}

}
>>>>>>> branch 'master' of https://github.com/simondai13/Projects-DS.git
