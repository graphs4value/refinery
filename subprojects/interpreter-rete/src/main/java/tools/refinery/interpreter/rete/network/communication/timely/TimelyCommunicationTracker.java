/*******************************************************************************
 * Copyright (c) 2010-2019, Tamas Szabo, Istvan Rath and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.network.communication.timely;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.index.IndexerListener;
import tools.refinery.interpreter.rete.index.SpecializedProjectionIndexer;
import tools.refinery.interpreter.rete.index.SpecializedProjectionIndexer.ListenerSubscription;
import tools.refinery.interpreter.rete.index.StandardIndexer;
import tools.refinery.interpreter.rete.itc.alg.misc.topsort.TopologicalSorting;
import tools.refinery.interpreter.rete.itc.graphimpl.Graph;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration;
import tools.refinery.interpreter.rete.matcher.TimelyConfiguration.TimelineRepresentation;
import tools.refinery.interpreter.rete.network.NetworkStructureChangeSensitiveNode;
import tools.refinery.interpreter.rete.network.Node;
import tools.refinery.interpreter.rete.network.ProductionNode;
import tools.refinery.interpreter.rete.network.StandardNode;
import tools.refinery.interpreter.rete.network.communication.CommunicationGroup;
import tools.refinery.interpreter.rete.network.communication.CommunicationTracker;
import tools.refinery.interpreter.rete.network.communication.MessageSelector;
import tools.refinery.interpreter.rete.network.communication.NodeComparator;
import tools.refinery.interpreter.rete.network.mailbox.Mailbox;
import tools.refinery.interpreter.rete.single.DiscriminatorDispatcherNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

/**
 * Timely (DDF) implementation of the {@link CommunicationTracker}.
 *
 * @author Tamas Szabo
 * @since 2.3
 */
public class TimelyCommunicationTracker extends CommunicationTracker {

    protected final TimelyConfiguration configuration;

    public TimelyCommunicationTracker(final Logger logger, final TimelyConfiguration configuration) {
		super(logger);
        this.configuration = configuration;
    }

    @Override
    protected CommunicationGroup createGroup(final Node representative, final int index) {
        final boolean isSingleton = isSingleton(representative);
        return new TimelyCommunicationGroup(this, representative, index, isSingleton);
    }

    @Override
    protected void reconstructQueueContents(final Set<CommunicationGroup> oldActiveGroups) {
        for (final CommunicationGroup oldGroup : oldActiveGroups) {
            for (final Entry<MessageSelector, Collection<Mailbox>> entry : oldGroup.getMailboxes().entrySet()) {
                for (final Mailbox mailbox : entry.getValue()) {
                    final CommunicationGroup newGroup = this.groupMap.get(mailbox.getReceiver());
                    newGroup.notifyHasMessage(mailbox, entry.getKey());
                }
            }
        }
    }

    @Override
    public Mailbox proxifyMailbox(final Node requester, final Mailbox original) {
        final Mailbox mailboxToProxify = (original instanceof TimelyMailboxProxy)
                ? ((TimelyMailboxProxy) original).getWrappedMailbox()
                : original;
        final TimestampTransformation preprocessor = getPreprocessor(requester, mailboxToProxify.getReceiver());
        if (preprocessor == null) {
            return mailboxToProxify;
        } else {
            return new TimelyMailboxProxy(mailboxToProxify, preprocessor);
        }
    }

    @Override
    public IndexerListener proxifyIndexerListener(final Node requester, final IndexerListener original) {
        final IndexerListener listenerToProxify = (original instanceof TimelyIndexerListenerProxy)
                ? ((TimelyIndexerListenerProxy) original).getWrappedIndexerListener()
                : original;
        final TimestampTransformation preprocessor = getPreprocessor(requester, listenerToProxify.getOwner());
        if (preprocessor == null) {
            return listenerToProxify;
        } else {
            return new TimelyIndexerListenerProxy(listenerToProxify, preprocessor);
        }
    }

    protected TimestampTransformation getPreprocessor(final Node source, final Node target) {
        final Node effectiveSource = source instanceof SpecializedProjectionIndexer
                ? ((SpecializedProjectionIndexer) source).getActiveNode()
                : source;
        final CommunicationGroup sourceGroup = this.getGroup(effectiveSource);
        final CommunicationGroup targetGroup = this.getGroup(target);

        if (sourceGroup != null && targetGroup != null) {
            // during RETE construction, the groups may be still null
            if (sourceGroup != targetGroup && sourceGroup.isRecursive()) {
                // targetGroup is a successor SCC of sourceGroup
                // and sourceGroup is a recursive SCC
                // then we need to zero out the timestamps
                return TimestampTransformation.RESET;
            }
            if (sourceGroup == targetGroup && target instanceof ProductionNode) {
                // if requester and receiver are in the same SCC
                // and receiver is a production node
                // then we need to increment the timestamps
                return TimestampTransformation.INCREMENT;
            }
        }

        return null;
    }

    @Override
    protected void postProcessNode(final Node node) {
        if (node instanceof NetworkStructureChangeSensitiveNode) {
            ((NetworkStructureChangeSensitiveNode) node).networkStructureChanged();
        }
    }

    @Override
    protected void postProcessGroup(final CommunicationGroup group) {
        if (this.configuration.getTimelineRepresentation() == TimelineRepresentation.FAITHFUL) {
            final Node representative = group.getRepresentative();
            final Set<Node> groupMembers = getPartition(representative);
            if (groupMembers != null && groupMembers.size() > 1) {
                final Graph<Node> graph = new Graph<Node>();

                for (final Node node : groupMembers) {
                    graph.insertNode(node);
                }

                for (final Node source : groupMembers) {
                    for (final Node target : this.dependencyGraph.getTargetNodes(source)) {
                        // (1) the edge is not a recursion cut point
                        // (2) the edge is within this group
                        if (!this.isRecursionCutPoint(source, target) && groupMembers.contains(target)) {
                            graph.insertEdge(source, target);
                        }
                    }
                }

                final List<Node> orderedNodes = TopologicalSorting.compute(graph);
                final Map<Node, Integer> nodeMap = CollectionsFactory.createMap();
                int identifier = 0;
                for (final Node orderedNode : orderedNodes) {
                    nodeMap.put(orderedNode, identifier++);
                }

                ((TimelyCommunicationGroup) group).setComparatorAndReorderMailboxes(new NodeComparator(nodeMap));
            }
        }
    }

    /**
     * This static field is used for debug purposes in the DotGenerator.
     */
    public static final Function<Node, Function<Node, String>> EDGE_LABEL_FUNCTION = new Function<Node, Function<Node, String>>() {

        @Override
        public Function<Node, String> apply(final Node source) {
            return new Function<Node, String>() {
                @Override
                public String apply(final Node target) {
                    if (source instanceof SpecializedProjectionIndexer) {
                        final Collection<ListenerSubscription> subscriptions = ((SpecializedProjectionIndexer) source)
                                .getSubscriptions();
                        for (final ListenerSubscription subscription : subscriptions) {
                            if (subscription.getListener().getOwner() == target
                                    && subscription.getListener() instanceof TimelyIndexerListenerProxy) {
                                return ((TimelyIndexerListenerProxy) subscription.getListener()).preprocessor
                                        .toString();
                            }
                        }
                    }
                    if (source instanceof StandardIndexer) {
                        final Collection<IndexerListener> listeners = ((StandardIndexer) source).getListeners();
                        for (final IndexerListener listener : listeners) {
                            if (listener.getOwner() == target && listener instanceof TimelyIndexerListenerProxy) {
                                return ((TimelyIndexerListenerProxy) listener).preprocessor.toString();
                            }
                        }
                    }
                    if (source instanceof StandardNode) {
                        final Collection<Mailbox> mailboxes = ((StandardNode) source).getChildMailboxes();
                        for (final Mailbox mailbox : mailboxes) {
                            if (mailbox.getReceiver() == target && mailbox instanceof TimelyMailboxProxy) {
                                return ((TimelyMailboxProxy) mailbox).preprocessor.toString();
                            }
                        }
                    }
                    if (source instanceof DiscriminatorDispatcherNode) {
                        final Collection<Mailbox> mailboxes = ((DiscriminatorDispatcherNode) source)
                                .getBucketMailboxes().values();
                        for (final Mailbox mailbox : mailboxes) {
                            if (mailbox.getReceiver() == target && mailbox instanceof TimelyMailboxProxy) {
                                return ((TimelyMailboxProxy) mailbox).preprocessor.toString();
                            }
                        }
                    }
                    return null;
                }
            };
        }

    };

}
