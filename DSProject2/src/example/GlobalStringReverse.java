package example;

import java.util.List;

import rmi_framework.RemoteObj;

public interface GlobalStringReverse extends RemoteObj{

	public List<String> globalReverse(List<String> l, StringReverse reverser) throws IndexOutOfBoundsException;
}
