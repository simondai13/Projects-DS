import java.net.InetSocketAddress;

public class RemoteObjectRef
{
	InetSocketAddress address;
    long id;

    public RemoteObjectRef(InetSocketAddress address, long id) 
    {
    	this.address=address;
    	this.id=id;
    }

    // this method is important, since it is a stub creator.
    // 
    Object localise()
    {
	// Implement this as you like: essentially you should 
	// create a new stub object and returns it.
	// Assume the stub class has the name e.g.
	//
	//       Remote_Interface_Name + "_stub".
	//
	// Then you can create a new stub as follows:
	// 
	//       Class c = Class.forName(Remote_Interface_Name + "_stub");
	//       Object o = c.newinstance()
	//
	// For this to work, your stub should have a constructor without arguments.
	// You know what it does when it is called: it gives communication module
	// all what it got (use CM's static methods), including its method name, 
	// arguments etc., in a marshalled form, and CM (yourRMI) sends it out to 
	// another place. 
	// Here let it return null.
	return null;
    }
}