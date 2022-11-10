package tools.refinery.store.model.representation;

import tools.refinery.store.map.ContinousHashProvider;

public abstract sealed class DataRepresentation<K, V> permits Relation, AuxiliaryData {
	private final String name;

	private final V defaultValue;

	private final Class<K> keyType;

	private final Class<V> valueType;

	protected DataRepresentation(String name, Class<K> keyType, Class<V> valueType, V defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
		this.keyType = keyType;
		this.valueType = valueType;
	}

	public String getName() {
		return name;
	}

	public abstract ContinousHashProvider<K> getHashProvider();

	public abstract boolean isValidKey(K key);

	public V getDefaultValue() {
		return defaultValue;
	}

	public Class<K> getKeyType() {
		return keyType;
	}

	public Class<V> getValueType() {
		return valueType;
	}
}
