import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Comparator;


public class NodeCompare implements Comparator<InetSocketAddress>,Serializable  {
	private static final long serialVersionUID = 1L;

	@Override
	public int compare(InetSocketAddress n1, InetSocketAddress n2) {
		if(n2.getAddress() ==null)
			System.out.println("Problem");
		int ipCmp= n1.getAddress().getHostAddress().compareTo(n2.getAddress().getHostAddress());
		if(ipCmp==0)
			return Integer.valueOf(n1.getPort()).compareTo(Integer.valueOf(n2.getPort()));
		
		return ipCmp;
			
	}

}
