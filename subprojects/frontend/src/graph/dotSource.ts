/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type {
  NodeMetadata,
  RelationMetadata,
} from '../xtext/xtextServiceResults';

import type GraphStore from './GraphStore';

const EDGE_WEIGHT = 1;
const CONTAINMENT_WEIGHT = 5;
const UNKNOWN_WEIGHT_FACTOR = 0.5;

function nodeName(graph: GraphStore, metadata: NodeMetadata): string {
  const name = graph.getName(metadata);
  switch (metadata.kind) {
    case 'INDIVIDUAL':
      return `<b>${name}</b>`;
    default:
      return name;
  }
}

function relationName(graph: GraphStore, metadata: RelationMetadata): string {
  const name = graph.getName(metadata);
  const { detail } = metadata;
  if (detail.type === 'class' && detail.abstractClass) {
    return `<i>${name}</i>`;
  }
  if (detail.type === 'reference' && detail.containment) {
    return `<b>${name}</b>`;
  }
  return name;
}

interface NodeData {
  isolated: boolean;
  exists: string;
  equalsSelf: string;
  unaryPredicates: Map<RelationMetadata, string>;
  count: string;
}

function computeNodeData(graph: GraphStore): NodeData[] {
  const {
    semantics: { nodes, relations, partialInterpretation },
  } = graph;

  const nodeData = Array.from(Array(nodes.length)).map(() => ({
    isolated: true,
    exists: 'FALSE',
    equalsSelf: 'FALSE',
    unaryPredicates: new Map(),
    count: '[0]',
  }));

  relations.forEach((relation) => {
    const visibility = graph.getVisibility(relation.name);
    if (visibility === 'none') {
      return;
    }
    const { arity } = relation;
    const interpretation = partialInterpretation[relation.name] ?? [];
    interpretation.forEach((tuple) => {
      const value = tuple[arity];
      if (visibility !== 'all' && value === 'UNKNOWN') {
        return;
      }
      for (let i = 0; i < arity; i += 1) {
        const index = tuple[i];
        if (typeof index === 'number') {
          const data = nodeData[index];
          if (data !== undefined) {
            data.isolated = false;
            if (arity === 1) {
              data.unaryPredicates.set(relation, value);
            }
          }
        }
      }
    });
  });

  partialInterpretation['builtin::exists']?.forEach(([index, value]) => {
    if (typeof index === 'number' && typeof value === 'string') {
      const data = nodeData[index];
      if (data !== undefined) {
        data.exists = value;
      }
    }
  });

  partialInterpretation['builtin::equals']?.forEach(([index, other, value]) => {
    if (
      typeof index === 'number' &&
      index === other &&
      typeof value === 'string'
    ) {
      const data = nodeData[index];
      if (data !== undefined) {
        data.equalsSelf = value;
      }
    }
  });

  partialInterpretation['builtin::count']?.forEach(([index, value]) => {
    if (typeof index === 'number' && typeof value === 'string') {
      const data = nodeData[index];
      if (data !== undefined) {
        data.count = value;
      }
    }
  });

  return nodeData;
}

function createNodes(
  graph: GraphStore,
  nodeData: NodeData[],
  lines: string[],
): void {
  const {
    semantics: { nodes },
    scopes,
  } = graph;

  nodes.forEach((node, i) => {
    const data = nodeData[i];
    if (data === undefined || data.isolated || data.exists === 'FALSE') {
      return;
    }
    const classList = [
      `node-${node.kind}`,
      `node-exists-${data.exists}`,
      `node-equalsSelf-${data.equalsSelf}`,
    ];
    if (data.unaryPredicates.size === 0) {
      classList.push('node-empty');
    }
    if (node.typeHash !== undefined) {
      classList.push(`node-typeHash-${node.typeHash}`);
    }
    const classes = classList.join(' ');
    const name = nodeName(graph, node);
    const border = node.kind === 'INDIVIDUAL' ? 2 : 1;
    const count = scopes ? ` ${data.count}` : '';
    lines.push(`n${i} [id="${node.name}", class="${classes}", label=<
        <table border="${border}" cellborder="0" cellspacing="0" style="rounded" bgcolor="white">
          <tr><td cellpadding="4.5" width="32" bgcolor="green">${name}${count}</td></tr>`);
    if (data.unaryPredicates.size > 0) {
      lines.push(
        '<hr/><tr><td cellpadding="4.5"><table fixedsize="TRUE" align="left" border="0" cellborder="0" cellspacing="0" cellpadding="1.5">',
      );
      data.unaryPredicates.forEach((value, relation) => {
        lines.push(
          `<tr>
              <td><img src="#${value}"/></td>
              <td width="1.5"></td>
              <td align="left" href="#${value}" id="${node.name},${
                relation.name
              },label">${relationName(graph, relation)}</td>
            </tr>`,
        );
      });
      lines.push('</table></td></tr>');
    }
    lines.push('</table>>]');
  });
}

function compare(
  a: readonly (number | string)[],
  b: readonly number[],
): number {
  if (a.length !== b.length + 1) {
    throw new Error('Tuple length mismatch');
  }
  for (let i = 0; i < b.length; i += 1) {
    const aItem = a[i];
    const bItem = b[i];
    if (typeof aItem !== 'number' || typeof bItem !== 'number') {
      throw new Error('Invalid tuple');
    }
    if (aItem < bItem) {
      return -1;
    }
    if (aItem > bItem) {
      return 1;
    }
  }
  return 0;
}

function binarySerach(
  tuples: readonly (readonly (number | string)[])[],
  key: readonly number[],
): string | undefined {
  let lower = 0;
  let upper = tuples.length - 1;
  while (lower <= upper) {
    const middle = Math.floor((lower + upper) / 2);
    const tuple = tuples[middle];
    if (tuple === undefined) {
      throw new Error('Range error');
    }
    const result = compare(tuple, key);
    if (result === 0) {
      const found = tuple[key.length];
      if (typeof found !== 'string') {
        throw new Error('Invalid tuple value');
      }
      return found;
    }
    if (result < 0) {
      lower = middle + 1;
    } else {
      // result > 0
      upper = middle - 1;
    }
  }
  return undefined;
}

function createRelationEdges(
  graph: GraphStore,
  nodeData: NodeData[],
  relation: RelationMetadata,
  showUnknown: boolean,
  lines: string[],
): void {
  const {
    semantics: { nodes, partialInterpretation },
  } = graph;
  const { detail } = relation;

  let constraint: 'true' | 'false' = 'true';
  let weight = EDGE_WEIGHT;
  let penwidth = 1;
  const name = graph.getName(relation);
  let label = `"${name}"`;
  if (detail.type === 'reference' && detail.containment) {
    weight = CONTAINMENT_WEIGHT;
    label = `<<b>${name}</b>>`;
    penwidth = 2;
  } else if (
    detail.type === 'opposite' &&
    graph.getVisibility(detail.opposite) !== 'none'
  ) {
    constraint = 'false';
    weight = 0;
  }

  const tuples = partialInterpretation[relation.name] ?? [];
  tuples.forEach(([from, to, value]) => {
    const isUnknown = value === 'UNKNOWN';
    if (
      (!showUnknown && isUnknown) ||
      typeof from !== 'number' ||
      typeof to !== 'number' ||
      typeof value !== 'string'
    ) {
      return;
    }

    const fromNode = nodes[from];
    const toNode = nodes[to];
    if (fromNode === undefined || toNode === undefined) {
      return;
    }

    const fromData = nodeData[from];
    const toData = nodeData[to];
    if (
      fromData === undefined ||
      fromData.exists === 'FALSE' ||
      toData === undefined ||
      toData.exists === 'FALSE'
    ) {
      return;
    }

    let dir = 'forward';
    let edgeConstraint = constraint;
    let edgeWeight = weight;
    const opposite = binarySerach(tuples, [to, from]);
    const oppositeUnknown = opposite === 'UNKNOWN';
    const oppositeSet = opposite !== undefined;
    const oppositeVisible = oppositeSet && (showUnknown || !oppositeUnknown);
    if (opposite === value) {
      if (to < from) {
        // We already added this edge in the reverse direction.
        return;
      }
      if (to > from) {
        dir = 'both';
      }
    } else if (oppositeVisible && to < from) {
      // Let the opposite edge drive the graph layout.
      edgeConstraint = 'false';
      edgeWeight = 0;
    } else if (isUnknown && (!oppositeSet || oppositeUnknown)) {
      // Only apply the UNKNOWN value penalty if we aren't the opposite
      // edge driving the graph layout from above, or the penalty would
      // be applied anyway.
      edgeWeight *= UNKNOWN_WEIGHT_FACTOR;
    }

    lines.push(`n${from} -> n${to} [
      id="${fromNode.name},${toNode.name},${relation.name}",
      dir="${dir}",
      constraint=${edgeConstraint},
      weight=${edgeWeight},
      xlabel=${label},
      penwidth=${penwidth},
      arrowsize=${penwidth >= 2 ? 0.875 : 1},
      style="${isUnknown ? 'dashed' : 'solid'}",
      class="edge-${value}"
    ]`);
  });
}

function createEdges(
  graph: GraphStore,
  nodeData: NodeData[],
  lines: string[],
): void {
  const {
    semantics: { relations },
  } = graph;
  relations.forEach((relation) => {
    if (relation.arity !== 2) {
      return;
    }
    const visibility = graph.getVisibility(relation.name);
    if (visibility !== 'none') {
      createRelationEdges(
        graph,
        nodeData,
        relation,
        visibility === 'all',
        lines,
      );
    }
  });
}

export default function dotSource(
  graph: GraphStore | undefined,
): [string, number] | undefined {
  if (graph === undefined) {
    return undefined;
  }
  const lines = [
    'digraph {',
    'graph [bgcolor=transparent];',
    `node [fontsize=12, shape=plain, fontname="OpenSans"];`,
    'edge [fontsize=10.5, color=black, fontname="OpenSans"];',
  ];
  const nodeData = computeNodeData(graph);
  createNodes(graph, nodeData, lines);
  createEdges(graph, nodeData, lines);
  lines.push('}');
  return [lines.join('\n'), lines.length];
}
