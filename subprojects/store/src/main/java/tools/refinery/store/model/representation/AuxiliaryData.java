package tools.refinery.store.model.representation;

import tools.refinery.store.map.ContinousHashProvider;

public final class AuxiliaryData<K, V> extends DataRepresentation<K, V> {
	private final ContinousHashProvider<K> hashProvider;

	public AuxiliaryData(String name, ContinousHashProvider<K> hashProvider, V defaultValue) {
		super(name, defaultValue);
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
