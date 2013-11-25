public class KTuple implements Comparable<KTuple>{

	
	private int k;
	private double[] values;
	
	
	public KTuple(int k, String s){
		
		this.k = k;
		String[] split = (s.replaceAll("[()]", "")).split(",");
		values = new double[k];
		for(int i = 0; i < k; i++){
			String num = split[i].trim();
			values[i] = Double.parseDouble(num);
		}
	}
	public KTuple(double[] point){
		
		k = point.length;
		values = new double[k];
		for(int i = 0; i < k; i++)
			values[i] = point[i];
	}
	
	
	public int getK(){
		
		return k;
	}
	public double getValue(int i){
		
		return values[i];
	}

	@Override
	public int compareTo(KTuple other) {
		
		if(k > other.k)
			return 1;
		else if(k < other.k)
			return -1;
		
		for(int i  = 0; i < k; i++){
			
			if(values[i] > other.values[i] + .01)
				return 1;
			else if(values[i] < other.values[i] - .01)
				return -1;
		}
		return 0;
	}
	
	public String toString(){
		
		String toReturn = "(";
		for(int i = 0; i < k-1; i++)
			toReturn += values[i]+", ";
		toReturn += values[k-1];
		return toReturn + ")";
	}
}
