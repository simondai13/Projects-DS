import java.util.List;


public class KTuple implements Comparable{

	
	private int k;
	private double[] values;
	
	
	public KTuple(int k, String s){
		
		this.k = k;
		String[] split = (s.replaceAll("[()]", "")).split(",");
		for(int i = 0; i < k; i++){
			
			values[i] = Double.parseDouble(split[i]);
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
	public int compareTo(Object otherPoint) {

		KTuple other = (KTuple)otherPoint;
		
		if(k > other.k)
			return 1;
		else if(k < other.k)
			return -1;
		
		for(int i  = 0; i < k; i++){
			
			if(values[i] > other.values[i])
				return 1;
			else if(values[i] < other.values[i])
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
