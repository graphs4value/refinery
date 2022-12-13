package tools.refinery.store.map;

public sealed interface AnyVersionedMap extends Versioned permits VersionedMap {
	long getSize();
}
