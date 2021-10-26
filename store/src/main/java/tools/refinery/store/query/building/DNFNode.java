package tools.refinery.store.query.building;

public class DNFNode {
	private final int id;
	private final String name;

	public DNFNode(int id, String name) {
		super();
		this.id = id;
		this.name = name;
	}
	
	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
