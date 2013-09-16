import java.io.Serializable;

/*Any class that wishes to be migrated must implement the MigratableProcess 
 * Interface.  When suspend is called from a separate thread of execution than the thread running
 * the process, the run() function must return shortly thereafter.  When the process is resumed by the process
 * manager, run will be called again.
 */
public interface MigratableProcess extends Runnable, Serializable{

	public void suspend();
}
