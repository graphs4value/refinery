/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import * as d3 from 'd3';
import { type Graphviz, graphviz } from 'd3-graphviz';
import type { BaseType, Selection } from 'd3-selection';
import { reaction, type IReactionDisposer } from 'mobx';
import { useCallback, useRef, useState } from 'react';
import { useResizeDetector } from 'react-resize-detector';

import { useRootStore } from '../RootStoreProvider';
import type { SemanticsSuccessResult } from '../xtext/xtextServiceResults';

function toGraphviz(
  semantics: SemanticsSuccessResult | undefined,
): string | undefined {
  if (semantics === undefined) {
    return undefined;
  }
  const lines = [
    'digraph {',
    'graph [bgcolor=transparent];',
    'node [fontsize=16, shape=plain];',
    'edge [fontsize=12, color=black];',
  ];
  const nodeIds = semantics.nodes.map((name, i) => name ?? `n${i}`);
  lines.push(
    ...nodeIds.map(
      (id, i) =>
        `n${i} [id="${id}", label=<<table border="1" cellborder="0" cellspacing="0" cellpadding="4" style="rounded" bgcolor="green"><tr><td>${id}</td></tr><hr/><tr><td bgcolor="white">node</td></tr></table>>];`,
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

interface Transform {
  x: number;
  y: number;
  k: number;
}

export default function GraphArea(): JSX.Element {
  const { editorStore } = useRootStore();
  const disposerRef = useRef<IReactionDisposer | undefined>();
  const graphvizRef = useRef<
    Graphviz<BaseType, unknown, null, undefined> | undefined
  >();
  const canvasRef = useRef<HTMLDivElement | undefined>();
  const zoomRef = useRef<
    d3.ZoomBehavior<HTMLDivElement, unknown> | undefined
  >();
  const [zoom, setZoom] = useState<Transform>({ x: 0, y: 0, k: 1 });
  const widthRef = useRef<number | undefined>();
  const heightRef = useRef<number | undefined>();

  const onResize = useCallback(
    (width: number | undefined, height: number | undefined) => {
      if (canvasRef.current === undefined || zoomRef.current === undefined) {
        return;
      }
      let moveX = 0;
      let moveY = 0;
      if (widthRef.current !== undefined && width !== undefined) {
        moveX = (width - widthRef.current) / 2;
      }
      if (heightRef.current !== undefined && height !== undefined) {
        moveY = (height - heightRef.current) / 2;
      }
      widthRef.current = width;
      heightRef.current = height;
      if (moveX === 0 && moveY === 0) {
        return;
      }
      const currentTransform = d3.zoomTransform(canvasRef.current);
      zoomRef.current.translateBy(
        d3.select(canvasRef.current),
        moveX / currentTransform.k - moveX,
        moveY / currentTransform.k - moveY,
      );
    },
    [],
  );

  const { ref: setCanvasResize } = useResizeDetector({
    onResize,
  });

  const setCanvas = useCallback(
    (element: HTMLDivElement | null) => {
      canvasRef.current = element ?? undefined;
      setCanvasResize(element);
      if (element === null) {
        return;
      }
      const zoomBehavior = d3.zoom<HTMLDivElement, unknown>();
      zoomBehavior.on(
        'zoom',
        (event: d3.D3ZoomEvent<HTMLDivElement, unknown>) =>
          setZoom(event.transform),
      );
      d3.select(element).call(zoomBehavior);
      zoomRef.current = zoomBehavior;
    },
    [setCanvasResize],
  );

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
          d3.transition().duration(300).ease(d3.easeCubic);
        /* eslint-disable-next-line @typescript-eslint/no-unsafe-argument,
          @typescript-eslint/no-explicit-any --
          Workaround for error in `@types/d3-graphviz`.
        */
        renderer.transition(transition as any);
        renderer.on(
          'postProcessSVG',
          // @ts-expect-error Custom `d3-graphviz` hook not covered by typings.
          (
            svgSelection: Selection<SVGSVGElement, unknown, BaseType, unknown>,
          ) => {
            svgSelection.selectAll('title').remove();
            const svg = svgSelection.node();
            if (svg === null) {
              return;
            }
            svg.querySelectorAll('.node').forEach((node) => {
              node.querySelectorAll('path').forEach((path) => {
                const d = path.getAttribute('d') ?? '';
                const points = d.split(/[A-Z ]/);
                points.shift();
                const x = points.map((p) => {
                  return Number(p.split(',')[0] ?? 0);
                });
                const y = points.map((p) => {
                  return Number(p.split(',')[1] ?? 0);
                });
                const xmin = Math.min.apply(null, x);
                const xmax = Math.max.apply(null, x);
                const ymin = Math.min.apply(null, y);
                const ymax = Math.max.apply(null, y);
                const rect = document.createElementNS(
                  'http://www.w3.org/2000/svg',
                  'rect',
                );
                rect.setAttribute('fill', path.getAttribute('fill') ?? '');
                rect.setAttribute('stroke', path.getAttribute('stroke') ?? '');
                rect.setAttribute('x', String(xmin));
                rect.setAttribute('y', String(ymin));
                rect.setAttribute('width', String(xmax - xmin));
                rect.setAttribute('height', String(ymax - ymin));
                rect.setAttribute('height', String(ymax - ymin));
                rect.setAttribute('rx', '12');
                rect.setAttribute('ry', '12');
                node.replaceChild(rect, path);
              });
            });
          },
        );
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
    [editorStore],
  );

  return (
    <Box
      sx={(theme) => ({
        width: '100%',
        height: '100%',
        position: 'relative',
        overflow: 'hidden',
        '& svg': {
          userSelect: 'none',
          '& .node': {
            '& text': {
              ...theme.typography.body2,
              fill: theme.palette.text.primary,
            },
            '& [stroke="black"]': {
              stroke: theme.palette.text.primary,
            },
            '& [fill="green"]': {
              fill:
                theme.palette.mode === 'dark'
                  ? theme.palette.primary.dark
                  : theme.palette.primary.light,
            },
            '& [fill="white"]': {
              fill: theme.palette.background.default,
              stroke: theme.palette.background.default,
            },
          },
          '& .edge': {
            '& text': {
              ...theme.typography.caption,
              fill: theme.palette.text.primary,
            },
            '& [stroke="black"]': {
              stroke: theme.palette.text.primary,
            },
            '& [fill="black"]': {
              fill: theme.palette.text.primary,
            },
          },
          '& .edge-UNKNOWN': {
            '& text': {
              fill: theme.palette.text.secondary,
            },
            '& [stroke="black"]': {
              stroke: theme.palette.text.secondary,
            },
            '& [fill="black"]': {
              fill: theme.palette.text.secondary,
            },
          },
          '& .edge-ERROR': {
            '& text': {
              fill: theme.palette.error.main,
            },
            '& [stroke="black"]': {
              stroke: theme.palette.error.main,
            },
            '& [fill="black"]': {
              fill: theme.palette.error.main,
            },
          },
        },
      })}
      ref={setCanvas}
    >
      <Box
        sx={{
          position: 'absolute',
          top: `${50 * zoom.k}%`,
          left: `${50 * zoom.k}%`,
          transform: `
              translate(${zoom.x}px, ${zoom.y}px)
              scale(${zoom.k})
              translate(-50%, -50%)
            `,
          transformOrigin: '0 0',
        }}
        ref={setElement}
      />
    </Box>
  );
}
