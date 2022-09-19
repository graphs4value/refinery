package tools.refinery.language.utils;

import java.util.ArrayList;
import java.util.Collection;

import tools.refinery.language.model.problem.NodeValueAssertion;

public record NodeInfo(String name, boolean individual, Collection<NodeValueAssertion> valueAssertions) {
	public NodeInfo(String name, boolean individual) {
		this(name, individual, new ArrayList<>());
	}
}
