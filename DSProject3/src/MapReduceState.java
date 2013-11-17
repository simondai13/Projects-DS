import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*This message type holds all the information a user
 * might want about the current state of a MapReduce facility
 * They simply need to use the MapReduceManager.getState() and they 
 * can pick through this data as they see fit
 */
public class MapReduceState implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public Map<InetSocketAddress,List<Task>> activeJobs;
	public Set<InetSocketAddress> activeNodes;
	public Map<String,List<InetSocketAddress>> fileLocs;
	
	public boolean isDone;
}
