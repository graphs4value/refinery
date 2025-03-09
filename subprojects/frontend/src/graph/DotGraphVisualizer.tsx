/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { useMediaQuery } from '@mui/system';
import { clsx } from 'clsx';
import * as d3 from 'd3';
import { type Graphviz, graphviz } from 'd3-graphviz';
import type { BaseType, Selection } from 'd3-selection';
import { reaction, type IReactionDisposer } from 'mobx';
import { observer } from 'mobx-react-lite';
import { useCallback, useRef, useState } from 'react';

import getLogger from '../utils/getLogger';

import type GraphStore from './GraphStore';
import GraphTheme from './GraphTheme';
import { FitZoomCallback } from './ZoomCanvas';
import dotSource from './dotSource';
import postProcessSvg from './postProcessSVG';

const LOG = getLogger('graph.DotGraphVisualizer');

function ptToPx(pt: number): number {
  return (pt * 4) / 3;
}

function DotGraphVisualizer({
  graph,
  fitZoom,
  transitionTime,
  animateThreshold,
  setSvgContainer,
  simplify,
}: {
  graph: GraphStore;
  fitZoom?: FitZoomCallback;
  transitionTime?: number;
  animateThreshold?: number;
  setSvgContainer?: (container: HTMLElement | undefined) => void;
  simplify?: boolean;
}): React.ReactElement {
  const transitionTimeOrDefault = transitionTime ?? 250;
  const animateThresholdOrDefault = animateThreshold ?? 100;
  const disposerRef = useRef<IReactionDisposer | undefined>(undefined);
  const graphvizRef = useRef<
    Graphviz<BaseType, unknown, null, undefined> | undefined
  >(undefined);
  const [animate, setAnimate] = useState(true);
  const [concretize, setConcretize] = useState(false);
  const prefersReducedMotion = useMediaQuery(
    '(prefers-reduced-motion: reduce)',
  );

  const setElement = useCallback(
    (element: HTMLDivElement | null) => {
      setSvgContainer?.(element ?? undefined);
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
        ['TRUE', 'UNKNOWN', 'ERROR'].forEach((icon) =>
          renderer.addImage(`#${icon}`, 16, 16),
        );
        renderer.zoom(false);
        renderer.tweenPrecision('5%');
        renderer.tweenShapes(false);
        renderer.convertEqualSidedPolygons(false);
        if (animate) {
          const transition = () =>
            d3
              .transition()
              .duration(transitionTimeOrDefault)
              .ease(d3.easeCubic);
          /* eslint-disable-next-line @typescript-eslint/no-unsafe-argument,
            @typescript-eslint/no-explicit-any --
            Workaround for error in `@types/d3-graphviz`.
          */
          renderer.transition(transition as any);
        } else {
          renderer.tweenPaths(false);
        }
        let newViewBox = { width: 0, height: 0 };
        renderer.onerror(LOG.error.bind(LOG));
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
              // Do not trigger fit zoom.
              newViewBox = { width: 0, height: 0 };
            }
          },
        );
        renderer.on('renderEnd', () => {
          // `d3-graphviz` uses `<title>` elements for traceability,
          // so we only remove them after the rendering is finished.
          d3.select(element).selectAll('title').remove();
          setConcretize(graph.concretize);
        });
        if (fitZoom !== undefined) {
          if (animate) {
            renderer.on('transitionStart', () => fitZoom(newViewBox));
          } else {
            renderer.on('end', () => fitZoom(false));
          }
        }
        disposerRef.current = reaction(
          () => dotSource(graph),
          (result) => {
            if (result === undefined) {
              return;
            }
            const [source, size] = result;
            // Disable tweening for large graphs to improve performance.
            // See https://github.com/magjac/d3-graphviz/issues/232#issuecomment-1157555213
            const newAnimate =
              size < animateThresholdOrDefault && !prefersReducedMotion;
            if (animate === newAnimate) {
              renderer.renderDot(source);
            } else {
              setAnimate(newAnimate);
            }
          },
          { fireImmediately: true },
        );
        graphvizRef.current = renderer;
      }
    },
    [
      graph,
      fitZoom,
      transitionTimeOrDefault,
      animateThresholdOrDefault,
      prefersReducedMotion,
      animate,
      setSvgContainer,
    ],
  );

  return (
    <GraphTheme
      className={clsx({ simplified: simplify, dimmed: graph.dimView })}
      ref={setElement}
      colorNodes={graph.colorNodes}
      hexTypeHashes={graph.hexTypeHashes}
      concretize={concretize}
    />
  );
}

export default observer(DotGraphVisualizer);
