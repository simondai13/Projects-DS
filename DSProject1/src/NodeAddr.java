import java.io.Serializable;
import java.net.InetAddress;


public class NodeAddr implements Serializable  {
	public InetAddress address;
	public int port;
	NodeAddr(InetAddress address, int port){
		this.address=address;
		this.port=port;
	}
}
