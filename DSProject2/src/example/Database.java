package example;

import rmi_framework.RemoteObj;

public interface Database extends RemoteObj {
	public String getName(int id);
	public void setName(int id, String name);
	//For illustration purposes
	public void printContents();
	
	//Copies the contents of this database into d. (this database is given priority
	//(for id's)  Note that this operation
	//would be more efficient without RMI, but it serves as a 
	//rigorous test of passing Remote Objects as parameters
	public void copyTo(Database d);
}
