import java.io.Serializable;

//Packaging for a migratable process that ships its GUID alongside the MigratableProcess
public class TaggedMP implements Serializable {
	public long id;
	public MigratableProcess mp;
	public TaggedMP(MigratableProcess mp, long id)
	{
		this.mp=mp;
		this.id=id;
	}
}
