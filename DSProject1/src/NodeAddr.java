import java.io.Serializable;


public class NodeAddr implements Serializable  {
	public String address;
	public int port;
	NodeAddr(String address, int port){
		this.address=address;
		this.port=port;
	}
}
