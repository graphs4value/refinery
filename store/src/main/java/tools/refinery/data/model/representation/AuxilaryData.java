package tools.refinery.data.model.representation;

import tools.refinery.data.map.ContinousHashProvider;

public class AuxilaryData<K,V> extends DataRepresentation<K, V> {
	private final String name;

	public AuxilaryData(String name, ContinousHashProvider<K> hashProvider,	V defaultValue) {
		super(hashProvider, defaultValue);
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean isValidKey(K key) {
		return true;
	}
}
