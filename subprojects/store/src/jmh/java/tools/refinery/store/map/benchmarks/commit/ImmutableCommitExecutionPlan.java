package tools.refinery.store.map.benchmarks.commit;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.map.internal.VersionedMapImpl;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@State(Scope.Benchmark)
public class ImmutableCommitExecutionPlan {

	@Param({ "16", "32", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384" })
	public int nCommit;

	@Param({ "32", "1000", "100000" })
	public int nKeys;

	@Param({ "2", "3" })
	public int nValues;

	private Random random;

	private String[] values;

	private VersionedMap<Integer, String> versionedMap;

	private Map<Integer, String> hashMap;

	private List<Map<Integer, String>> store;

	private ContinousHashProvider<Integer> hashProvider = MapTestEnvironment.prepareHashProvider(false);

	@Setup(Level.Trial)
	public void setUpTrial() {
		random = new Random();
		values = MapTestEnvironment.prepareValues(nValues);

		hashMap = new HashMap<>();
		store = new ArrayList<>();

		versionedMap = createSut();
		for (int key = 0; key < nKeys; key++) {
			String value = nextValue();
			versionedMap.put(key, value);
			hashMap.put(key, value);
		}
	}

	public VersionedMapImpl<Integer, String> createSut() {
		VersionedMapStore<Integer, String> store = new VersionedMapStoreImpl<Integer, String>(hashProvider, values[0]);
		return (VersionedMapImpl<Integer, String>) store.createMap();
	}

	public String nextValue() {
		return values[random.nextInt(nValues)];
	}

	public VersionedMap<Integer, String> getSut() {
		return versionedMap;
	}

	public Map<Integer, String> getFilledHashMap() {
		return hashMap;
	}


	public List<Map<Integer, String>> getStore() {
		return store;
	}
}
