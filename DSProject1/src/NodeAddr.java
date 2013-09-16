import java.io.Serializable;
import java.net.InetAddress;

/*
 * This is a simple class for packaging information on a node in the MigratableProcess distributed system.
 * It contains an IP address for the machine as well as a port number on which the node communicates.
 */
public class NodeAddr implements Serializable  {
	public InetAddress address;
	public int port;
	
	NodeAddr(InetAddress address, int port){
		this.address=address;
		this.port=port;
	}
}
