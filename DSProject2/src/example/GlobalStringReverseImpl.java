package example;

import java.util.ArrayList;
import java.util.List;

public class GlobalStringReverseImpl implements GlobalStringReverse{

	//number of characters to reverse
	private int reverseIndex;
	public String name;

	public GlobalStringReverseImpl(int rev){

		name = "";
		reverseIndex = rev;
	}
	public GlobalStringReverseImpl(int rev, String name){

		this.name = name;
		reverseIndex = rev;
	}
	
	public String getRMIName(){
		return name;
	}
	
	public List<String> globalReverse(List<String> l, StringReverse reverser)throws IndexOutOfBoundsException{

		List<String> toReturn = new ArrayList<String>();
		for(String s : l){
			
			if(s.length() < reverseIndex)
				throw new IndexOutOfBoundsException("reverseIndex too large for String " + s);
			else
				toReturn.add(reverser.reverse(s, s.length()));
		}
		
		return toReturn;
	}
}
