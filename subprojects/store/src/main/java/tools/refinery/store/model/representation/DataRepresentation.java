package tools.refinery.store.model.representation;

import tools.refinery.store.map.ContinousHashProvider;

public abstract sealed class DataRepresentation<K, V> permits Relation, AuxiliaryData {
	private final String name;

	private final V defaultValue;

	protected DataRepresentation(String name, V defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public String getName() {
		return name;
	}

	public abstract ContinousHashProvider<K> getHashProvider();

	public abstract boolean isValidKey(K key);

	public V getDefaultValue() {
		return defaultValue;
	}
}
