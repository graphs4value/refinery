/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import AddIcon from '@mui/icons-material/Add';
import CropFreeIcon from '@mui/icons-material/CropFree';
import RemoveIcon from '@mui/icons-material/Remove';
import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import { useTheme } from '@mui/material/styles';
import { CSSProperties } from '@mui/material/styles/createTypography';
import * as d3 from 'd3';
import { type Graphviz, graphviz } from 'd3-graphviz';
import type { BaseType, Selection } from 'd3-selection';
import { zoom as d3Zoom } from 'd3-zoom';
import { reaction, type IReactionDisposer } from 'mobx';
import { useCallback, useRef, useState } from 'react';

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
  const theme = useTheme();
  const disposerRef = useRef<IReactionDisposer | undefined>();
  const graphvizRef = useRef<
    Graphviz<BaseType, unknown, null, undefined> | undefined
  >();
  const canvasRef = useRef<HTMLDivElement | undefined>();
  const elementRef = useRef<HTMLDivElement | undefined>();
  const zoomRef = useRef<
    d3.ZoomBehavior<HTMLDivElement, unknown> | undefined
  >();
  const [zoom, setZoom] = useState<Transform>({ x: 0, y: 0, k: 1 });

  const setCanvas = useCallback((element: HTMLDivElement | null) => {
    canvasRef.current = element ?? undefined;
    if (element === null) {
      return;
    }
    const zoomBehavior = d3Zoom<HTMLDivElement, unknown>();
    // `@types/d3-zoom` does not contain the `center` function, because it is
    // only available as a pull request for `d3-zoom`.
    (
      zoomBehavior as unknown as {
        center(callback: (event: MouseEvent) => [number, number]): unknown;
      }
    ).center((event: MouseEvent | Touch) => {
      const { width, height } = element.getBoundingClientRect();
      const [x, y] = d3.pointer(event, element);
      return [x - width / 2, y - height / 2];
    });
    // Custom `centroid` method added via patch.
    (
      zoomBehavior as unknown as {
        centroid(centroid: [number, number]): unknown;
      }
    ).centroid([0, 0]);
    zoomBehavior.on('zoom', (event: d3.D3ZoomEvent<HTMLDivElement, unknown>) =>
      setZoom(event.transform),
    );
    d3.select(element).call(zoomBehavior);
    zoomRef.current = zoomBehavior;
  }, []);

  const setElement = useCallback(
    (element: HTMLDivElement | null) => {
      elementRef.current = element ?? undefined;
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
                rect.setAttribute('rx', '8');
                rect.setAttribute('ry', '8');
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

  const changeZoom = useCallback((event: React.MouseEvent, factor: number) => {
    if (canvasRef.current === undefined || zoomRef.current === undefined) {
      return;
    }
    const selection = d3.select(canvasRef.current);
    const zoomTransition = selection.transition().duration(250);
    const center: [number, number] = [0, 0];
    zoomRef.current.scaleBy(zoomTransition, factor, center);
    event.preventDefault();
    event.stopPropagation();
  }, []);

  const fitZoom = useCallback((event: React.MouseEvent) => {
    if (
      canvasRef.current === undefined ||
      zoomRef.current === undefined ||
      elementRef.current === undefined
    ) {
      return;
    }
    const { width: canvasWidth, height: canvasHeight } =
      canvasRef.current.getBoundingClientRect();
    const { width: scaledWidth, height: scaledHeight } =
      elementRef.current.getBoundingClientRect();
    const currentFactor = d3.zoomTransform(canvasRef.current).k;
    const width = scaledWidth / currentFactor;
    const height = scaledHeight / currentFactor;
    if (width > 0 && height > 0) {
      const factor = Math.min(
        1.0,
        (canvasWidth - 64) / width,
        (canvasHeight - 64) / height,
      );
      const selection = d3.select(canvasRef.current);
      const zoomTransition = selection.transition().duration(250);
      zoomRef.current.transform(zoomTransition, d3.zoomIdentity.scale(factor));
    }
    event.preventDefault();
    event.stopPropagation();
  }, []);

  return (
    <Box
      sx={{
        width: '100%',
        height: '100%',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          overflow: 'hidden',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
        }}
        ref={setCanvas}
      >
        <Box
          sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: `
              translate(${zoom.x}px, ${zoom.y}px)
              scale(${zoom.k})
              translate(-50%, -50%)
            `,
            transformOrigin: '0 0',
            '& svg': {
              userSelect: 'none',
              '& .node': {
                '& text': {
                  ...(theme.typography.body2 as Omit<
                    CSSProperties,
                    '@font-face'
                  >),
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
                  ...(theme.typography.caption as Omit<
                    CSSProperties,
                    '@font-face'
                  >),
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
          }}
          ref={setElement}
        />
      </Box>
      <Stack
        direction="column"
        p={1}
        sx={{ position: 'absolute', bottom: 0, right: 0 }}
      >
        <IconButton
          aria-label="Zoom in"
          onClick={(event) => changeZoom(event, 2)}
        >
          <AddIcon fontSize="small" />
        </IconButton>
        <IconButton
          aria-label="Zoom out"
          onClick={(event) => changeZoom(event, 0.5)}
        >
          <RemoveIcon fontSize="small" />
        </IconButton>
        <IconButton aria-label="Fit screen" onClick={fitZoom}>
          <CropFreeIcon fontSize="small" />
        </IconButton>
      </Stack>
    </Box>
  );
}
