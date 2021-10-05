package tools.refinery.store.query.building;

public class Variable {
	private final String name;
	private final String uniqueName;

	public Variable(String name) {
		super();
		this.name = name;
		this.uniqueName = DNFPredicate.generateUniqueName(name, "variable");
		
	}
	public String getName() {
		return name;
	}
	public String getUniqueName() {
		return uniqueName;
	}
	public boolean isNamed() {
		return name != null;
	}
}
