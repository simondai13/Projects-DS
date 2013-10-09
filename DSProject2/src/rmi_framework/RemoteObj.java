package rmi_framework;
//Empty highest level abstraction for a remote object, roughly equivalent to JAVA's rmi
//"Remote" interface.  Every remote object must have a single method "getName" that returns 
//a unique string identifier
public interface RemoteObj {
	public String getRMIName();
}
