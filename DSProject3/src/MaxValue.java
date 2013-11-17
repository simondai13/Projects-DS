import java.util.List;


public class MaxValue implements MapReducer{

	@Override
	//identity map
	public String map(String line) {

		return line;
	}

	@Override
	public String[] reduce(String[] lines) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> reduce(List<String> lines, String line) {

		if(lines.isEmpty()){
			
			lines.add(line);
			return lines;
		}
		
		String l = lines.get(0);
		int comparison = l.compareTo(line);
		if(comparison == 0){
		
			lines.add(l);
		}
		else if(comparison < 0){
			
			lines.clear();
			lines.add(line);
		}

		return lines;
	}

	
}
