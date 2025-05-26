/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BinaryOperator;

class TreapAggregate<R, T extends Comparable<? super T>> implements StatefulAggregate<R, T> {
	private final ToMonoid<T, R> toMonoid;
	private final R unit;
	private final BinaryOperator<R> monoid;
	private @Nullable Node<T, R> root;

	public TreapAggregate(ToMonoid<T, R> toMonoid, R unit, BinaryOperator<R> monoid) {
		this(toMonoid, unit, monoid, null);
	}

	private TreapAggregate(ToMonoid<T, R> toMonoid, R unit, BinaryOperator<R> monoid, @Nullable Node<T, R> root) {
		this.toMonoid = toMonoid;
		this.unit = unit;
		this.monoid = monoid;
		this.root = root;
	}

	@Override
	public void add(T value) {
		add(value, 1);
	}

	public void add(T value, int count) {
		var split = split(root, value);
		root = join(split.left, split.key, split.hash, split.count + count, split.right);
	}

	@Override
	public void remove(T value) {
		var split = split(root, value);
		root = join(split.left, split.key, split.hash, split.count - 1, split.right);
	}

	@Override
	public @NotNull R getResult() {
		return root == null ? unit : root.aggregate;
	}

	@Override
	public boolean isEmpty() {
		return root == null;
	}

	@Override
	public StatefulAggregate<R, T> deepCopy() {
		return new TreapAggregate<>(toMonoid, unit, monoid, root);
	}

	boolean validate(TreapAggregate<R, T> other) {
		if (root == null) {
			return other.root == null;
		}
		return root.validate(other.root);
	}

	private SplitResult<T, R> split(@Nullable Node<T, R> node, T key) {
		if (node == null) {
			return new SplitResult<>(key);
		}
		int compare = key.compareTo(node.key);
		if (compare == 0) {
			return new SplitResult<>(node.key, node.hash, node.count, node.left, node.right);
		}
		if (compare < 0) {
			var leftSplit = split(node.left, key);
			return new SplitResult<>(leftSplit.key, leftSplit.hash, leftSplit.count, leftSplit.left,
					join(leftSplit.right, node.key, node.hash, node.count, node.right));
		}
		var rightSplit = split(node.right, key);
		return new SplitResult<>(rightSplit.key, rightSplit.hash, rightSplit.count,
				join(node.left, node.key, node.hash, node.count, rightSplit.left), rightSplit.right);
	}

	private @Nullable Node<T, R> join(@Nullable Node<T, R> left, T key, int hash, int count,
									  @Nullable Node<T, R> right) {
		if (count < 0) {
			throw new IllegalArgumentException("count must be positive");
		}
		if (count == 0) {
			return merge(left, right);
		}
		if (right == null) {
			if (left == null) {
				return new Node<>(key, hash, count, toMonoid);
			}
			if (comparePriority(key, hash, left)) {
				return new Node<>(key, hash, count, left, null, toMonoid, monoid);
			}
			return new Node<>(left.key, left.hash, left.count, left.left, join(left.right, key, hash, count, null),
					toMonoid, monoid);
		}
		if (left == null) {
			if (comparePriority(key, hash, right)) {
				return new Node<>(key, hash, count, null, right, toMonoid, monoid);
			}
			return new Node<>(right.key, right.hash, right.count, join(null, key, hash, count, right.left),
					right.right, toMonoid, monoid);
		}
		if (comparePriority(key, hash, left) && comparePriority(key, hash, right)) {
			return new Node<>(key, hash, count, left, right, toMonoid, monoid);
		}
		if (comparePriority(left, right)) {
			return new Node<>(left.key, left.hash, left.count, left.left, join(left.right, key, hash, count, right),
					toMonoid, monoid);
		}
		return new Node<>(right.key, right.hash, right.count, join(left, key, hash, count, right.left), right.right,
				toMonoid, monoid);
	}

	private @Nullable Node<T, R> merge(@Nullable Node<T, R> left, @Nullable Node<T, R> right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		if (comparePriority(left, right)) {
			return new Node<>(left.key, left.hash, left.count, left.left, merge(left.right, right), toMonoid, monoid);
		}
		return new Node<>(right.key, right.hash, right.count, merge(left, right.left), right.right, toMonoid, monoid);
	}

	private boolean comparePriority(@NotNull Node<T, R> left, @NotNull Node<T, R> right) {
		return comparePriority(left.key, left.hash, right);
	}

	private boolean comparePriority(T key, int hash, @NotNull Node<T, R> right) {
		return hash < right.hash || (hash == right.hash && key.compareTo(right.key) < 0);
	}

	private static int murmur3Like(Object key) {
		int k = key.hashCode() * 0xcc9e2d51;
		k = Integer.rotateLeft(k, 15);
		return k * 0x1b873593;
	}

	private static class Node<T, R> {
		private final int hash;
		private final T key;
		private final int count;
		private final R aggregate;
		private final @Nullable Node<T, R> left;
		private final @Nullable Node<T, R> right;

		public Node(T key, int hash, int count, ToMonoid<T, R> toMonoid) {
			this.key = key;
			this.hash = hash;
			this.count = count;
			aggregate = toMonoid.apply(count, key);
			left = null;
			right = null;
		}

		private Node(T key, int hash, int count, @Nullable Node<T, R> left, @Nullable Node<T, R> right,
					 ToMonoid<T, R> toMonoid, BinaryOperator<R> monoid) {
			this.key = key;
			this.hash = hash;
			this.count = count;
			var aggregate = toMonoid.apply(count, key);
			if (left != null) {
				aggregate = monoid.apply(left.aggregate, aggregate);
			}
			if (right != null) {
				aggregate = monoid.apply(aggregate, right.aggregate);
			}
			this.aggregate = aggregate;
			this.left = left;
			this.right = right;
		}

		private boolean validate(Node<T, R> other) {
			if (other == null ||hash != other.hash || !Objects.equals(key, other.key) || count != other.count ||
					!Objects.equals(aggregate, other.aggregate)) {
				return false;
			}
			if (left == null) {
                if (other.left != null) {
					return false;
				}
            } else  {
				if (!left.validate(other.left)) {
					return false;
				}
			}
			if (right == null) {
				return other.right == null;
			} else {
				return right.validate(other.right);
			}
		}
	}

	private record SplitResult<T, R>(T key, int hash, int count, @Nullable Node<T, R> left,
									 @Nullable Node<T, R> right) {
		public SplitResult(T key) {
			this(key, murmur3Like(key), 0, null, null);
		}
	}
}
