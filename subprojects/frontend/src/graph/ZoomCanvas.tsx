/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import * as d3 from 'd3';
import { zoom as d3Zoom } from 'd3-zoom';
import React, { useCallback, useRef, useState } from 'react';
import { useResizeDetector } from 'react-resize-detector';

import ZoomButtons from './ZoomButtons';

declare module 'd3-zoom' {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- Redeclaring type parameters.
  interface ZoomBehavior<ZoomRefElement extends Element, Datum> {
    // `@types/d3-zoom` does not contain the `center` function, because it is
    // only available as a pull request for `d3-zoom`.
    center(callback: (event: MouseEvent | Touch) => [number, number]): this;

    // Custom `centroid` method added via patch.
    centroid(centroid: [number, number]): this;
  }
}

interface Transform {
  x: number;
  y: number;
  k: number;
}

export type ChangeZoomCallback = (factor: number) => void;

export type SetFitZoomCallback = (fitZoom: boolean) => void;

export type FitZoomCallback = ((newSize?: {
  width: number;
  height: number;
}) => void) &
  ((newSize: boolean) => void);

export default function ZoomCanvas({
  children,
  fitPadding,
  transitionTime,
}: {
  children?: React.ReactNode | ((fitZoom: FitZoomCallback) => React.ReactNode);
  fitPadding?: number;
  transitionTime?: number;
}): JSX.Element {
  const fitPaddingOrDefault = fitPadding ?? ZoomCanvas.defaultProps.fitPadding;
  const transitionTimeOrDefault =
    transitionTime ?? ZoomCanvas.defaultProps.transitionTime;

  const canvasRef = useRef<HTMLDivElement | undefined>();
  const elementRef = useRef<HTMLDivElement | undefined>();
  const zoomRef = useRef<
    d3.ZoomBehavior<HTMLDivElement, unknown> | undefined
  >();
  const [zoom, setZoom] = useState<Transform>({ x: 0, y: 0, k: 1 });
  const [fitZoom, setFitZoom] = useState(true);
  const fitZoomRef = useRef(fitZoom);

  const makeTransition = useCallback(
    (element: HTMLDivElement) =>
      d3.select(element).transition().duration(transitionTimeOrDefault),
    [transitionTimeOrDefault],
  );

  const fitZoomCallback = useCallback<FitZoomCallback>(
    (newSize) => {
      if (
        !fitZoomRef.current ||
        canvasRef.current === undefined ||
        zoomRef.current === undefined ||
        elementRef.current === undefined
      ) {
        return;
      }
      let width = 0;
      let height = 0;
      if (newSize === undefined || typeof newSize === 'boolean') {
        const elementRect = elementRef.current.getBoundingClientRect();
        const currentFactor = d3.zoomTransform(canvasRef.current).k;
        width = elementRect.width / currentFactor;
        height = elementRect.height / currentFactor;
      } else {
        ({ width, height } = newSize);
      }
      if (width === 0 || height === 0) {
        return;
      }
      const canvasRect = canvasRef.current.getBoundingClientRect();
      const factor = Math.min(
        1.0,
        (canvasRect.width - 2 * fitPaddingOrDefault) / width,
        (canvasRect.height - 2 * fitPaddingOrDefault) / height,
      );
      const target =
        newSize === false
          ? d3.select(canvasRef.current)
          : makeTransition(canvasRef.current);
      zoomRef.current.transform(target, d3.zoomIdentity.scale(factor));
    },
    [fitPaddingOrDefault, makeTransition],
  );

  const setFitZoomCallback = useCallback<SetFitZoomCallback>(
    (newFitZoom) => {
      setFitZoom(newFitZoom);
      fitZoomRef.current = newFitZoom;
      if (newFitZoom) {
        fitZoomCallback();
      }
    },
    [fitZoomCallback],
  );

  const changeZoomCallback = useCallback<ChangeZoomCallback>(
    (factor) => {
      setFitZoomCallback(false);
      if (canvasRef.current === undefined || zoomRef.current === undefined) {
        return;
      }
      const zoomTransition = makeTransition(canvasRef.current);
      const center: [number, number] = [0, 0];
      zoomRef.current.scaleBy(zoomTransition, factor, center);
    },
    [makeTransition, setFitZoomCallback],
  );

  const onResize = useCallback(() => fitZoomCallback(), [fitZoomCallback]);

  const { ref: resizeRef } = useResizeDetector({
    onResize,
    refreshMode: 'debounce',
    refreshRate: transitionTimeOrDefault,
  });

  const setCanvas = useCallback(
    (canvas: HTMLDivElement | null) => {
      canvasRef.current = canvas ?? undefined;
      resizeRef(canvas);
      if (canvas === null) {
        return;
      }
      const zoomBehavior = d3Zoom<HTMLDivElement, unknown>()
        .duration(transitionTimeOrDefault)
        .center((event) => {
          const { width, height } = canvas.getBoundingClientRect();
          const [x, y] = d3.pointer(event, canvas);
          return [x - width / 2, y - height / 2];
        })
        .centroid([0, 0])
        .scaleExtent([1 / 32, 8]);
      zoomBehavior.on(
        'zoom',
        (event: d3.D3ZoomEvent<HTMLDivElement, unknown>) => {
          setZoom(event.transform);
          if (event.sourceEvent) {
            setFitZoomCallback(false);
          }
        },
      );
      d3.select(canvas).call(zoomBehavior);
      zoomRef.current = zoomBehavior;
    },
    [transitionTimeOrDefault, setFitZoomCallback, resizeRef],
  );

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
          }}
          ref={elementRef}
        >
          {typeof children === 'function'
            ? children(fitZoomCallback)
            : children}
        </Box>
      </Box>
      <ZoomButtons
        changeZoom={changeZoomCallback}
        fitZoom={fitZoom}
        setFitZoom={setFitZoomCallback}
      />
    </Box>
  );
}

ZoomCanvas.defaultProps = {
  children: undefined,
  fitPadding: 8,
  transitionTime: 250,
};
