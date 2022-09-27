package tools.refinery.store.model;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.tuple.Tuple;

public class TupleHashProviderBitMagic implements ContinousHashProvider<Tuple> {

	@Override
	public int getHash(Tuple key, int index) {
		if(key.getSize() == 1) {
			return key.get(0);
		}

		int result = 0;
		final int startBitIndex = index*30;
		final int finalBitIndex = startBitIndex+30;
		final int arity = key.getSize();

		for(int i = startBitIndex; i<=finalBitIndex; i++) {
			final int selectedKey = key.get(i%arity);
			final int selectedPosition = 1<<(i/arity);
			if((selectedKey&selectedPosition) != 0) {
				result |= 1<<(i%30);
			}
		}

		return result;
	}
}
