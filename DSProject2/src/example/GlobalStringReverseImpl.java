package example;

import java.util.ArrayList;
import java.util.List;

import rmi_framework.RemoteObj;

public class GlobalStringReverseImpl implements RemoteObj, GlobalStringReverse{

	//number of characters to reverse
	private int reverseIndex;
	
	public GlobalStringReverseImpl(int rev){

		reverseIndex = rev;
	}
	
	public List<String> globalReverse(List<String> l, StringReverseImpl reverser)throws IndexOutOfBoundsException{

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
