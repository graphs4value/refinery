/*
 * SPDX-FileCopyrightText: 2023-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { escape } from 'lodash-es';

import type {
  NodeMetadata,
  RelationMetadata,
} from '../xtext/xtextServiceResults';

import type GraphStore from './GraphStore';
import obfuscateColor from './obfuscateColor';
import { binarySearch, extractValue, extractValueColor } from './valueUtils';

const EDGE_WEIGHT = 1;
const CONTAINMENT_WEIGHT = 5;
const UNKNOWN_WEIGHT_FACTOR = 0.5;

function nodeName(
  graph: GraphStore,
  metadata: NodeMetadata,
  count?: string,
): string {
  let name = escape(graph.getName(metadata));
  if (count !== undefined) {
    name = `${name}&nbsp;[${count}]`;
  }
  switch (metadata.kind) {
    case 'atom':
      return `<b>${name}</b>`;
    default:
      return name;
  }
}

function relationName(graph: GraphStore, metadata: RelationMetadata): string {
  const name = escape(graph.getName(metadata));
  const { detail } = metadata;
  if (
    detail.type === 'class' &&
    detail.isAbstract &&
    !metadata.name.startsWith('builtin::')
  ) {
    return `<i>${name}</i>`;
  }
  if (detail.type === 'reference' && detail.isContainment) {
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
    exists: 'false',
    equalsSelf: 'false',
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
      if (visibility !== 'all' && extractValueColor(value) === 'unknown') {
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
    if (typeof index === 'number') {
      const data = nodeData[index];
      if (data !== undefined) {
        data.count = extractValue(value) ?? '0';
      }
    }
  });

  return nodeData;
}

/**
 * Escape an identifier so that it can be used as an SVG element `id` and as
 * an `url(#)` reference to a clip path.
 *
 * While colons are allowed in such IDs, quotes and percent signs are not.
 *
 * @param name The name to escape.
 * @returns The escaped name.
 */
function encodeName(name: string): string {
  return encodeURIComponent(name)
    .replaceAll('%3A', ':')
    .replaceAll('_', '__')
    .replaceAll("'", '_27')
    .replaceAll('(', '_28')
    .replaceAll(')', '_29')
    .replaceAll(',', '_2C')
    .replaceAll('%', '_');
}

let measureDiv: HTMLElement | undefined;

type SizeCache = Map<string, string>;

function setLabelWidth(
  text: string,
  align: 'left' | 'center' | 'right',
  size: number,
  sizeCache: SizeCache,
) {
  const key = `${text}|${align}|${size}`;
  let cached = sizeCache.get(key);
  if (cached === undefined) {
    if (measureDiv === undefined) {
      measureDiv = document.createElement('measureDiv');
      measureDiv.style.position = 'absolute';
      measureDiv.style.left = '-1000rem';
      measureDiv.style.top = '-1000rem';
      measureDiv.style.visibility = 'hidden';
      measureDiv.style.width = 'auto';
      measureDiv.style.height = 'auto';
      measureDiv.style.padding = '0';
      measureDiv.style.whiteSpace = 'pre';
      measureDiv.style.lineHeight = String(14 / 12);
      measureDiv.style.fontOpticalSizing = 'none';
      measureDiv.style.letterSpacing = '0';
      measureDiv.style.textRendering = 'geometricPreicision';
      measureDiv.ariaHidden = 'true';
      document.body.appendChild(measureDiv);
    }
    measureDiv.style.fontSize = `${size}pt`;
    measureDiv.innerHTML = text;
    const { width, height } = measureDiv.getBoundingClientRect();
    // Rounding the length (converted to points) to 1 decimal precision seems to yield the best alignment.
    // The rounding matters here, because Graphviz will also apply some rounding internall.
    cached = `<table align="${align}" fixedsize="true" width="${Math.ceil(width * 7.5) / 10}" height="${Math.ceil(height * 7.5) / 10}" border="0" cellborder="0" cellpadding="0" cellspacing="0">
            <tr><td>${text}</td></tr>
          </table>`;
    sizeCache.set(key, cached);
  }
  return cached;
}

function createNodes(
  graph: GraphStore,
  nodeData: NodeData[],
  lines: string[],
  sizeCache: SizeCache,
): void {
  const {
    semantics: { nodes },
    scopes,
    showNonExistent,
  } = graph;

  nodes.forEach((node, i) => {
    const data = nodeData[i];
    if (
      data === undefined ||
      data.isolated ||
      (!showNonExistent && data.exists === 'false')
    ) {
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
    if (node.color !== undefined) {
      classList.push(`node-typeHash-${obfuscateColor(node.color)}`);
    }
    const classes = classList.join(' ');
    const name = nodeName(graph, node, scopes ? data.count : undefined);
    const border = node.kind === 'atom' ? 2 : 1;
    const nodeLabel = setLabelWidth(name, 'center', 12, sizeCache);
    const encodedNodeName = encodeName(node.name);
    lines.push(`n${i} [id="${encodedNodeName}", class="${classes}", label=<
        <table border="${border}" cellborder="0" cellspacing="0" style="rounded" bgcolor="white">
          <tr><td cellpadding="4.5" width="32" bgcolor="green">${nodeLabel}</td></tr>`);
    if (data.unaryPredicates.size > 0) {
      lines.push(
        '<hr/><tr><td cellpadding="4.5"><table fixedsize="true" align="left" border="0" cellborder="0" cellspacing="0" cellpadding="1.5">',
      );
      data.unaryPredicates.forEach((value, relation) => {
        const encodedRelationName = `${encodedNodeName},${encodeName(relation.name)}`;
        if (relation.dataType === undefined) {
          const relationLabel = setLabelWidth(
            relationName(graph, relation),
            'left',
            12,
            sizeCache,
          );
          lines.push(
            `<tr>
                <td><img src="#${value}"/></td>
                <td width="1.5"></td>
                <td align="left" href="#${value}" id="${encodedRelationName},label">${relationLabel}</td>
              </tr>`,
          );
        } else {
          let valueString = extractValue(value);
          if (valueString === 'unknown') {
            valueString = '?';
          }
          const attributeName = graph.getName(relation);
          const attributeLabel = `${attributeName}: ${valueString}`;
          const color = extractValueColor(value);
          lines.push(
            `<tr>
                <td><img src="#attribute-${color}"/></td>
                <td width="1.5"></td>
                <td align="left" href="#${color}" id="${encodedRelationName},label">${attributeLabel}</td>
              </tr>`,
          );
        }
      });
      lines.push('</table></td></tr>');
    }
    lines.push('</table>>]');
  });
}

function getEdgeLabel(
  name: string,
  containment: boolean,
  value: string,
  sizeCache: SizeCache,
): string {
  const label = setLabelWidth(
    containment ? `<b>${name}</b>` : name,
    'left',
    10.5,
    sizeCache,
  );
  if (value !== 'error') {
    return `<${label}>`;
  }
  // No need to set an id for the image for animation,
  // because it will be the only `<use>` element in its group.
  return `<<table align="left" border="0" cellborder="0" cellspacing="0" cellpadding="0">
    <tr>
      <td><img src="#error"/></td>
      <td width="3.9375"></td>
      <td align="left">${label}</td>
    </tr>
  </table>>`;
}

function createRelationEdges(
  graph: GraphStore,
  nodeData: NodeData[],
  relation: RelationMetadata,
  showUnknown: boolean,
  lines: string[],
  sizeCache: SizeCache,
): void {
  const {
    semantics: { nodes, partialInterpretation },
    showNonExistent,
  } = graph;
  const { detail } = relation;

  let constraint: 'true' | 'false' = 'true';
  let weight = EDGE_WEIGHT;
  let penwidth = 1;
  const name = graph.getName(relation);
  let containment = false;
  if (detail.type === 'reference' && detail.isContainment) {
    weight = CONTAINMENT_WEIGHT;
    containment = true;
    penwidth = 2;
  } else if (
    detail.type === 'opposite' &&
    graph.getVisibility(detail.of) !== 'none'
  ) {
    constraint = 'false';
    weight = 0;
  }

  const tuples = partialInterpretation[relation.name] ?? [];
  const encodedRelation = encodeName(relation.name);
  tuples.forEach(([from, to, value]) => {
    const isUnknown = value === 'unknown';
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
      toData === undefined ||
      (!showNonExistent &&
        (fromData.exists === 'false' || toData.exists === 'false'))
    ) {
      return;
    }

    let dir = 'forward';
    let edgeConstraint = constraint;
    let edgeWeight = weight;
    const opposite = binarySearch(tuples, [to, from]);
    const oppositeUnknown = opposite === 'unknown';
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
      // Only apply the unknown value penalty if we aren't the opposite
      // edge driving the graph layout from above, or the penalty would
      // be applied anyway.
      edgeWeight *= UNKNOWN_WEIGHT_FACTOR;
    }

    const encodedFrom = encodeName(fromNode.name);
    const encodedTo = encodeName(toNode.name);
    const id = `${encodedFrom},${encodedTo},${encodedRelation}`;
    const label = getEdgeLabel(name, containment, value, sizeCache);
    lines.push(`n${from} -> n${to} [
      id="${id}",
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
  sizeCache: SizeCache,
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
        sizeCache,
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
  const sizeCache: SizeCache = new Map();
  createNodes(graph, nodeData, lines, sizeCache);
  createEdges(graph, nodeData, lines, sizeCache);
  lines.push('}');
  return [lines.join('\n'), lines.length];
}
