import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


//generates DNA data
public class DNAGenerator {

	//variance from median
	public static double prob = .2; 

	//args should be:
	//args[0] = Length of string
	//    [1] = Number of clusters
	//    [2] = Number of strings per cluster
	//    [3] = Output file
	public static void main(String[] args) throws IOException{
		
		int length = Integer.parseInt(args[0]);
		int numClusters = Integer.parseInt(args[1]);
		int numStrings = Integer.parseInt(args[2]);
		
		File f = new File(args[3]);
		if(!f.exists())
			f.createNewFile();
		
		List<String> sequences = new ArrayList<String>();
		
		//generate a cluster
		for(int i = 0; i < numClusters; i++){
			
			//make a median string by choosing a random string of {A,C,G,T}
			
			String median = "";
			for(int k = 0; k < length; k++)
				median += randChar();
			System.out.println("made one");
			//System.out.println("MEDIAN: + " + median);
			
			for(int j = 0; j < numStrings; j++){
				
				String seq = "";
				for(int k = 0; k < length; k++){
					
					if(Math.random() > prob){
						
						seq += median.charAt(k);
					}
					else{
						
						seq += randChar();
					}
				}
				
				sequences.add(seq);
			}
		}
		
		//write to file
		PrintWriter pw = new PrintWriter(f);
		Collections.shuffle(sequences);
		for(String s : sequences){
			
			pw.println(s);
			//System.out.println(s);
		}
		pw.close();
	}
	
	//returns a random DNA character
	public static char randChar(){
		
		int rand = (int) (Math.random()*4);

		if(rand == 0)
			return 'A';
		else if(rand == 1)
			return 'C';
		else if(rand == 2)
			return 'G';
		else
			return 'T';
			
	}
}
