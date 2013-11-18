import java.io.IOException;

public class MainNode {

	public static void main(String[] args) throws IOException{
	
    	SystemNode s1 = new SystemNode(Integer.parseInt(args[0]));
    	Thread t = new Thread(s1);
    	t.run();
	}
}
