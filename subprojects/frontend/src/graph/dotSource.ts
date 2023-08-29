/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
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

function nodeName({ simpleName, kind }: NodeMetadata): string {
  switch (kind) {
    case 'INDIVIDUAL':
      return `<b>${simpleName}</b>`;
    case 'NEW':
      return `<i>${simpleName}</i>`;
    default:
      return simpleName;
  }
}

function relationName({ simpleName, detail }: RelationMetadata): string {
  if (detail.type === 'class' && detail.abstractClass) {
    return `<i>${simpleName}</i>`;
  }
  if (detail.type === 'reference' && detail.containment) {
    return `<b>${simpleName}</b>`;
  }
  return simpleName;
}

interface NodeData {
  exists: string;
  equalsSelf: string;
  unaryPredicates: Map<RelationMetadata, string>;
}

function computeNodeData(graph: GraphStore): NodeData[] {
  const {
    semantics: { nodes, relations, partialInterpretation },
  } = graph;

  const nodeData = Array.from(Array(nodes.length)).map(() => ({
    exists: 'FALSE',
    equalsSelf: 'FALSE',
    unaryPredicates: new Map(),
  }));

  relations.forEach((relation) => {
    if (relation.arity !== 1) {
      return;
    }
    const visibility = graph.getVisiblity(relation.name);
    if (visibility === 'none') {
      return;
    }
    const interpretation = partialInterpretation[relation.name] ?? [];
    interpretation.forEach(([index, value]) => {
      if (
        typeof index === 'number' &&
        typeof value === 'string' &&
        (visibility === 'all' || value !== 'UNKNOWN')
      ) {
        nodeData[index]?.unaryPredicates?.set(relation, value);
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

  return nodeData;
}

function createNodes(graph: GraphStore, lines: string[]): void {
  const nodeData = computeNodeData(graph);
  const {
    semantics: { nodes },
  } = graph;

  nodes.forEach((node, i) => {
    const data = nodeData[i];
    if (data === undefined) {
      return;
    }
    const classes = [
      `node-${node.kind} node-exists-${data.exists} node-equalsSelf-${data.equalsSelf}`,
    ].join(' ');
    const name = nodeName(node);
    const border = node.kind === 'INDIVIDUAL' ? 2 : 1;
    lines.push(`n${i} [id="${node.name}", class="${classes}", label=<
        <table border="${border}" cellborder="0" cellspacing="0" style="rounded" bgcolor="white">
          <tr><td cellpadding="4.5" width="32" bgcolor="green">${name}</td></tr>`);
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
              },label">${relationName(relation)}</td>
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
  let label = `"${relation.simpleName}"`;
  if (detail.type === 'reference' && detail.containment) {
    weight = CONTAINMENT_WEIGHT;
    label = `<<b>${relation.simpleName}</b>>`;
    penwidth = 2;
  } else if (
    detail.type === 'opposite' &&
    graph.getVisiblity(detail.opposite) !== 'none'
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

function createEdges(graph: GraphStore, lines: string[]): void {
  const {
    semantics: { relations },
  } = graph;
  relations.forEach((relation) => {
    if (relation.arity !== 2) {
      return;
    }
    const visibility = graph.getVisiblity(relation.name);
    if (visibility !== 'none') {
      createRelationEdges(graph, relation, visibility === 'all', lines);
    }
  });
}

export default function dotSource(
  graph: GraphStore | undefined,
): string | undefined {
  if (graph === undefined) {
    return undefined;
  }
  const lines = [
    'digraph {',
    'graph [bgcolor=transparent];',
    `node [fontsize=12, shape=plain, fontname="OpenSans"];`,
    'edge [fontsize=10.5, color=black, fontname="OpenSans"];',
  ];
  createNodes(graph, lines);
  createEdges(graph, lines);
  lines.push('}');
  return lines.join('\n');
}
