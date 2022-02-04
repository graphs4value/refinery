package tools.refinery.store.map;

public interface Versioned {
	public long commit();
	//maybe revert()?
	public void restore(long state);
}
