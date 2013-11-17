import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class Main {
    public static void main(String[] args) throws IOException{
    	SystemNode s1 = new SystemNode(2342); //Master node
    	SystemNode s2 = new SystemNode(2344);
    	SystemNode s3 = new SystemNode(2347);
    	SystemNode s4 = new SystemNode(2350);
    	SystemNode s5 = new SystemNode(2353);
    	Thread t1 = new Thread(s1);
    	Thread t2 = new Thread(s2);
    	Thread t3 = new Thread(s3);
    	Thread t4 = new Thread(s4);
    	Thread t5 = new Thread(s5);
    	t1.start();
    	t2.start();
    	t3.start();
    	t4.start();
    	t5.start();
  
    	MapReduceManager mrm = new MapReduceManager();
    	mrm.configMapReduce(new File("testconfig.txt"));
    	mrm.startMapReduce(Histogram.class);
    	//System.out.println(MapReducer.class);
    }
}
