import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;


public class MasterConfiguration implements Serializable {
	private static final long serialVersionUID = 1L;
	Map<InetSocketAddress,Integer> dfsToMRports;
}
