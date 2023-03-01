package tools.refinery.store.map;

public final class Cursors {
    private Cursors() {
        throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
    }

    public static <K, V> Cursor<K, V> empty() {
        return new Empty<>();
    }

    private static class Empty<K, V> implements Cursor<K, V> {
        private boolean terminated = false;

        @Override
        public K getKey() {
            return null;
        }

        @Override
        public V getValue() {
            return null;
        }

        @Override
        public boolean isTerminated() {
            return terminated;
        }

        @Override
        public boolean move() {
            terminated = true;
            return false;
        }
    }
}
