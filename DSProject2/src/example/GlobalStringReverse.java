package example;

import java.util.ArrayList;
import java.util.List;

public class GlobalStringReverse {

	//number of characters to reverse
	private int reverseIndex;
	
	public GlobalStringReverse(int rev){

		reverseIndex = rev;
	}
	
	public List<String> globalReverse(List<String> l, StringReverse reverser){

		List<String> toReturn = new ArrayList<String>();
		for(String s : l){
			
			if(s.length() < reverseIndex)
				toReturn.add(reverser.reverse(s, reverseIndex));
			else
				toReturn.add(reverser.reverse(s, s.length()));
		}
		
		return toReturn;
	}
}
