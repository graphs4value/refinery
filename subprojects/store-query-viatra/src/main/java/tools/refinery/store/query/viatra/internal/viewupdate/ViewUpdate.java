package tools.refinery.store.query.viatra.internal.viewupdate;

import java.util.Arrays;
import java.util.Objects;

record ViewUpdate (Object[] tuple, boolean isInsertion) {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(tuple);
		result = prime * result + Objects.hash(isInsertion);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ViewUpdate other = (ViewUpdate) obj;
		return isInsertion == other.isInsertion && Arrays.deepEquals(tuple, other.tuple);
	}

	@Override
	public String toString() {
		return "ViewUpdate [" + Arrays.toString(tuple) + "insertion= "+this.isInsertion+"]";
	}

}
