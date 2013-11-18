import java.io.File;
import java.io.IOException;


public class MainHistogram {

    public static void main(String[] args) throws IOException{
    	
    	MapReduceManager mrm = new MapReduceManager();
    	mrm.configMapReduce(new File("confighistogram.txt"));
    	mrm.startMapReduce(Histogram.class);
    	//mrm.killMapReduce();
    	
    	if(!mrm.getStatus().isDone)
    		System.out.println("STILL WORKING");
    	
    	try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if(mrm.getStatus().isDone){
    		System.out.println("Done");
    	}
    }
}
