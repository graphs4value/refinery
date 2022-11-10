package tools.refinery.store.model.representation;

import tools.refinery.store.map.ContinousHashProvider;

public final class AuxiliaryData<K, V> extends DataRepresentation<K, V> {
	private final ContinousHashProvider<K> hashProvider;

	public AuxiliaryData(String name, Class<K> keyType, ContinousHashProvider<K> hashProvider, Class<V> valueType,
						 V defaultValue) {
		super(name, keyType, valueType, defaultValue);
		this.hashProvider = hashProvider;
	}

	@Override
	public ContinousHashProvider<K> getHashProvider() {
		return hashProvider;
	}

	@Override
	public boolean isValidKey(K key) {
		return true;
	}
}
