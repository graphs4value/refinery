package tools.refinery.data.map;

public interface Versioned {
	public long commit();
	//maybe revert()?
	public void restore(long state);
}
