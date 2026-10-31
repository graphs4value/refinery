/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import * as d3 from 'd3';
import { type Graphviz, graphviz } from 'd3-graphviz';
import type { BaseType, Selection } from 'd3-selection';
import { observer } from 'mobx-react-lite';
import { useCallback, useRef } from 'react';

import GraphTheme from '../graph/GraphTheme';
import dotSource from '../graph/dotSource';
import { exportBlob } from '../graph/export/exportDiagram';
import postProcessSvg, { addSVGIcons } from '../graph/postProcessSVG';

import type { Request } from './HeadlessStore';

function HeadlessGraphRenderer({ request }: { request: Request }) {
  const { graph, settings, pending } = request;

  const graphvizRef = useRef<
    Graphviz<BaseType, unknown, null, undefined> | undefined
  >(undefined);

  const setElement = useCallback(
    (element: HTMLDivElement | null) => {
      if (graphvizRef.current !== undefined) {
        // `@types/d3-graphviz` does not contain the signature for the `destroy` method.
        (graphvizRef.current as unknown as { destroy(): void }).destroy();
        graphvizRef.current = undefined;
      }
      if (element === null || !pending) {
        return;
      }
      element.replaceChildren();
      const renderer = graphviz(element) as Graphviz<
        BaseType,
        unknown,
        null,
        undefined
      >;
      renderer.keyMode('id');
      addSVGIcons(renderer);
      renderer.zoom(false);
      renderer.tweenShapes(false);
      renderer.tweenPaths(false);
      renderer.convertEqualSidedPolygons(false);
      renderer.onerror((err) => request.reject(err));
      renderer.on(
        'postProcessSVG',
        // @ts-expect-error Custom `d3-graphviz` hook not covered by typings.
        (
          svgSelection: Selection<SVGSVGElement, unknown, BaseType, unknown>,
        ) => {
          const svg = svgSelection.node();
          if (svg !== null) {
            try {
              postProcessSvg(svg);
            } catch (err) {
              request.reject(err);
            }
          }
        },
      );
      renderer.on('renderEnd', () => {
        // `d3-graphviz` uses `<title>` elements for traceability,
        // so we only remove them after the rendering is finished.
        d3.select(element).selectAll('title').remove();
        (async () => {
          const blob = await exportBlob(element, graph, settings);
          if (blob === undefined) {
            throw new Error('No graph was exported');
          }
          const bytes = await blob.bytes();
          // Only call resolve once we no longer need `element`, including the PNG readout,
          // because resolving the request will remove us from the DOM.
          request.resolve(bytes);
        })().catch((err) => request.reject(err));
      });
      graphvizRef.current = renderer;
      try {
        const source = dotSource(graph);
        if (source) {
          renderer.renderDot(source[0]);
        } else {
          request.reject(new Error('Could not convert graph into dot'));
        }
      } catch (err) {
        request.reject(err);
      }
    },
    [graph, settings, pending, request],
  );

  return (
    <GraphTheme
      ref={setElement}
      colorNodes={graph.colorNodes}
      hexTypeHashes={graph.hexTypeHashes}
    />
  );
}

export default observer(HeadlessGraphRenderer);
