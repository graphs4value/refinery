/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import * as d3 from 'd3';
import { type Graphviz, graphviz } from 'd3-graphviz';
import type { BaseType, Selection } from 'd3-selection';
import { reaction, type IReactionDisposer } from 'mobx';
import { useCallback, useRef } from 'react';

import { useRootStore } from '../RootStoreProvider';
import type { SemanticsSuccessResult } from '../xtext/xtextServiceResults';

import GraphTheme from './GraphTheme';
import { FitZoomCallback } from './ZoomCanvas';
import postProcessSvg from './postProcessSVG';

function toGraphviz(
  semantics: SemanticsSuccessResult | undefined,
): string | undefined {
  if (semantics === undefined) {
    return undefined;
  }
  const lines = [
    'digraph {',
    'graph [bgcolor=transparent];',
    `node [fontsize=12, shape=plain, fontname="OpenSans"];`,
    'edge [fontsize=10.5, color=black, fontname="OpenSans"];',
  ];
  const nodeIds = semantics.nodes.map((name, i) => name ?? `n${i}`);
  lines.push(
    ...nodeIds.map(
      (id, i) =>
        `n${i} [id="${id}", label=<<table border="1" cellborder="0" cellspacing="0" cellpadding="4.5" style="rounded" bgcolor="green"><tr><td>${id}</td></tr><hr/><tr><td bgcolor="white">node</td></tr></table>>];`,
    ),
  );
  Object.keys(semantics.partialInterpretation).forEach((relation) => {
    if (relation === 'builtin::equals' || relation === 'builtin::contains') {
      return;
    }
    const tuples = semantics.partialInterpretation[relation];
    if (tuples === undefined) {
      return;
    }
    const first = tuples[0];
    if (first === undefined || first.length !== 3) {
      return;
    }
    const nameFragments = relation.split('::');
    const simpleName = nameFragments[nameFragments.length - 1] ?? relation;
    lines.push(
      ...tuples.map(([from, to, value]) => {
        if (
          typeof from !== 'number' ||
          typeof to !== 'number' ||
          typeof value !== 'string'
        ) {
          return '';
        }
        const isUnknown = value === 'UNKNOWN';
        return `n${from} -> n${to} [
            id="${nodeIds[from]},${nodeIds[to]},${relation}",
            xlabel="${simpleName}",
            style="${isUnknown ? 'dashed' : 'solid'}",
            class="edge-${value}"
          ];`;
      }),
    );
  });
  lines.push('}');
  return lines.join('\n');
}

function ptToPx(pt: number): number {
  return (pt * 4) / 3;
}

export default function DotGraphVisualizer({
  fitZoom,
  transitionTime,
}: {
  fitZoom?: FitZoomCallback;
  transitionTime?: number;
}): JSX.Element {
  const transitionTimeOrDefault =
    transitionTime ?? DotGraphVisualizer.defaultProps.transitionTime;

  const { editorStore } = useRootStore();
  const disposerRef = useRef<IReactionDisposer | undefined>();
  const graphvizRef = useRef<
    Graphviz<BaseType, unknown, null, undefined> | undefined
  >();

  const setElement = useCallback(
    (element: HTMLDivElement | null) => {
      if (disposerRef.current !== undefined) {
        disposerRef.current();
        disposerRef.current = undefined;
      }
      if (graphvizRef.current !== undefined) {
        // `@types/d3-graphviz` does not contain the signature for the `destroy` method.
        (graphvizRef.current as unknown as { destroy(): void }).destroy();
        graphvizRef.current = undefined;
      }
      if (element !== null) {
        element.replaceChildren();
        const renderer = graphviz(element) as Graphviz<
          BaseType,
          unknown,
          null,
          undefined
        >;
        renderer.keyMode('id');
        renderer.zoom(false);
        renderer.tweenPrecision('5%');
        renderer.tweenShapes(false);
        renderer.convertEqualSidedPolygons(false);
        const transition = () =>
          d3.transition().duration(transitionTimeOrDefault).ease(d3.easeCubic);
        /* eslint-disable-next-line @typescript-eslint/no-unsafe-argument,
          @typescript-eslint/no-explicit-any --
          Workaround for error in `@types/d3-graphviz`.
        */
        renderer.transition(transition as any);
        let newViewBox = { width: 0, height: 0 };
        renderer.on(
          'postProcessSVG',
          // @ts-expect-error Custom `d3-graphviz` hook not covered by typings.
          (
            svgSelection: Selection<SVGSVGElement, unknown, BaseType, unknown>,
          ) => {
            const svg = svgSelection.node();
            if (svg !== null) {
              postProcessSvg(svg);
              newViewBox = {
                width: ptToPx(svg.viewBox.baseVal.width),
                height: ptToPx(svg.viewBox.baseVal.height),
              };
            } else {
              newViewBox = { width: 0, height: 0 };
            }
          },
        );
        if (fitZoom !== undefined) {
          renderer.on('transitionStart', () => fitZoom(newViewBox));
        }
        disposerRef.current = reaction(
          () => editorStore?.semantics,
          (semantics) => {
            const str = toGraphviz(semantics);
            if (str !== undefined) {
              renderer.renderDot(str);
            }
          },
          { fireImmediately: true },
        );
        graphvizRef.current = renderer;
      }
    },
    [editorStore, fitZoom, transitionTimeOrDefault],
  );

  return <GraphTheme ref={setElement} />;
}

DotGraphVisualizer.defaultProps = {
  fitZoom: undefined,
  transitionTime: 250,
};
