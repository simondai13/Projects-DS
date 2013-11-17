
import java.util.List;

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
	public List<String> reduce(List<String> lines, String line) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lines;
	}
	
	@Override
	public int partition(String in){
		return 0;
	}

}

