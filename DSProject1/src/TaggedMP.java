import java.io.Serializable;


public class TaggedMP implements Serializable {
	public long id;
	public MigratableProcess mp;
	public TaggedMP(MigratableProcess mp, long id)
	{
		this.mp=mp;
		this.id=id;
	}
}
