
import java.util.List;

//classical word count example
public class Histogram implements MapReducer {
	
	@Override
	//adds a 1 (single count)
	public String map(String line) {
		return line + " 1";
	}

	@Override
	//simply increments the counter if duplicate word, otherwise adds on the line
	public List<String> reduce(List<String> lines, String line) {
		
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

