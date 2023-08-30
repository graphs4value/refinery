/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import * as d3 from 'd3';
import { type Graphviz, graphviz } from 'd3-graphviz';
import type { BaseType, Selection } from 'd3-selection';
import { reaction, type IReactionDisposer } from 'mobx';
import { observer } from 'mobx-react-lite';
import { useCallback, useRef } from 'react';

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
}: {
  graph: GraphStore;
  fitZoom?: FitZoomCallback;
  transitionTime?: number;
}): JSX.Element {
  const transitionTimeOrDefault =
    transitionTime ?? DotGraphVisualizer.defaultProps.transitionTime;
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
        ['TRUE', 'UNKNOWN', 'ERROR'].forEach((icon) =>
          renderer.addImage(`#${icon}`, 16, 16),
        );
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
        });
        if (fitZoom !== undefined) {
          renderer.on('transitionStart', () => fitZoom(newViewBox));
        }
        disposerRef.current = reaction(
          () => dotSource(graph),
          (source) => {
            if (source !== undefined) {
              renderer.renderDot(source);
            }
          },
          { fireImmediately: true },
        );
        graphvizRef.current = renderer;
      }
    },
    [graph, fitZoom, transitionTimeOrDefault],
  );

  return <GraphTheme ref={setElement} />;
}

DotGraphVisualizer.defaultProps = {
  fitZoom: undefined,
  transitionTime: 250,
};

export default observer(DotGraphVisualizer);
