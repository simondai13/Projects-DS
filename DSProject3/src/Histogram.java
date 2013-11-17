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
