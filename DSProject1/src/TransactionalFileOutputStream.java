import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;


public class TransactionalFileOutputStream extends OutputStream implements
		Serializable {


	public TransactionalFileOutputStream(String filename, boolean someBool)
	{}
	
	
	@Override
	public void write(int b) throws IOException {
		// TODO Auto-generated method stub

	}

}
