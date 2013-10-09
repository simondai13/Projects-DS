package example;

import java.util.Map;
import java.util.TreeMap;


public class TreeDatabase implements Database{
	String name;
	TreeMap<Integer, String> db;
	
	public TreeDatabase(String dbName){
		this.name=dbName;
		db= new TreeMap<Integer,String>();
	}

	@Override
	public String getRMIName() {
		return name;
	}

	@Override
	public String getName(int id) {
		return db.get(id);
	}

	@Override
	public void setName(int id, String name) {
		db.put(id, name);		
	}

	@Override
	public void copyTo(Database d) {
		for (Map.Entry<Integer, String> e : db.entrySet()) {
		    d.setName(e.getKey(), e.getValue());
		}
	}
	
	@Override
	public void printContents(){
		for (Map.Entry<Integer, String> e : db.entrySet()) {
		   System.out.print(e.getKey());
		   System.out.print(" ");
		   System.out.print(e.getValue());
		   System.out.print("\n");
		}
	}
}

