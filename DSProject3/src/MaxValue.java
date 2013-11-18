import java.util.List;


public class MaxValue implements MapReducer{

	@Override
	//identity map
	public String map(String line) {

		return line;
	}

	public List<String> reduce(List<String> lines, String line) {

		if(lines.isEmpty()){
			
			lines.add(line);
			return lines;
		}
		
		String l = lines.get(0);
		int comparison = Integer.parseInt(l.trim()) - Integer.parseInt(line.trim());
		if(comparison == 0){
		
			lines.add(l);
		}
		else if(comparison < 0){
			
			lines.clear();
			lines.add(line);
		}

		return lines;
	}

	@Override
	public int partition(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
