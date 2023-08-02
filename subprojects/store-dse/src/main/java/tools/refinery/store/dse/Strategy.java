package tools.refinery.store.dse;

public interface Strategy {

	public void initStrategy(DesignSpaceExplorationAdapter designSpaceExplorationAdapter);

	public void explore();
}
