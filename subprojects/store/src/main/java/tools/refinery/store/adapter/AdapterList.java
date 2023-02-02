package tools.refinery.store.adapter;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class AdapterList<T> implements Iterable<T> {
	private final List<AnyModelAdapterType> adapterTypes;
	private final List<T> adapters;

	public AdapterList() {
		adapterTypes = new ArrayList<>();
		adapters = new ArrayList<>();
	}

	public AdapterList(int adapterCount) {
		adapterTypes = new ArrayList<>(adapterCount);
		adapters = new ArrayList<>(adapterCount);
	}

	public int size() {
		return adapters.size();
	}

	public void add(AnyModelAdapterType adapterType, T adapter) {
		adapterTypes.add(adapterType);
		adapters.add(adapter);
	}

	public <U extends T> Optional<U> tryGet(AnyModelAdapterType adapterType, Class<? extends U> adapterClass) {
		int size = size();
		for (int i = 0; i < size; i++) {
			if (getType(i).supports(adapterType)) {
				return Optional.of(adapterClass.cast(get(i)));
			}
		}
		return Optional.empty();
	}

	public <U extends T> U get(AnyModelAdapterType adapterType, Class<U> adapterClass) {
		return tryGet(adapterType, adapterClass).orElseThrow(() -> new IllegalArgumentException(
				"No %s was configured".formatted(adapterType)));
	}

	public AnyModelAdapterType getType(int i) {
		return adapterTypes.get(i);
	}

	public T get(int i) {
		return adapters.get(i);
	}

	public Collection<AnyModelAdapterType> getAdapterTypes() {
		return Collections.unmodifiableCollection(adapterTypes);
	}

	public Iterable<Entry<T>> withAdapterTypes() {
		return () -> new Iterator<>() {
			private int i = 0;

			@Override
			public boolean hasNext() {
				return i < size();
			}

			@Override
			public Entry<T> next() {
				if (i >= size()) {
					throw new NoSuchElementException();
				}
				var entry = new Entry<>(getType(i), get(i));
				i++;
				return entry;
			}
		};
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		return adapters.iterator();
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		adapters.forEach(action);
	}

	@Override
	public Spliterator<T> spliterator() {
		return adapters.spliterator();
	}

	public record Entry<T>(AnyModelAdapterType adapterType, T adapter) {
	}
}
