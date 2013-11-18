
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
		return line + " 1";
	}

	@Override
	public List<String> reduce(List<String> lines, String line) {
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] record = line.split(" ");
		String word = record[0];
		int count = Integer.parseInt(record[1]);
		for(int i = 0; i < lines.size() ; i++){

			String[] recordData = lines.get(i).split(" ");
			String w = recordData[0];
			int num = Integer.parseInt(recordData[1]);
			int total = num+count;
			if(w.equals(word)){
				lines.set(i, w+" "+total);
				return lines;
			}
			
		}
		lines.add(line);
		return lines;
	}
	
	@Override
	public int partition(String in){
		return 0;
	}

}

