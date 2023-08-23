/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import * as d3 from 'd3';
import { zoom as d3Zoom } from 'd3-zoom';
import React, { useCallback, useRef, useState } from 'react';

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

export default function ZoomCanvas({
  children,
  fitPadding,
  transitionTime,
}: {
  children?: React.ReactNode;
  fitPadding?: number;
  transitionTime?: number;
}): JSX.Element {
  const canvasRef = useRef<HTMLDivElement | undefined>();
  const elementRef = useRef<HTMLDivElement | undefined>();
  const zoomRef = useRef<
    d3.ZoomBehavior<HTMLDivElement, unknown> | undefined
  >();
  const fitPaddingOrDefault = fitPadding ?? ZoomCanvas.defaultProps.fitPadding;
  const transitionTimeOrDefault =
    transitionTime ?? ZoomCanvas.defaultProps.transitionTime;

  const [zoom, setZoom] = useState<Transform>({ x: 0, y: 0, k: 1 });

  const setCanvas = useCallback(
    (canvas: HTMLDivElement | null) => {
      canvasRef.current = canvas ?? undefined;
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
        .centroid([0, 0]);
      zoomBehavior.on(
        'zoom',
        (event: d3.D3ZoomEvent<HTMLDivElement, unknown>) =>
          setZoom(event.transform),
      );
      d3.select(canvas).call(zoomBehavior);
      zoomRef.current = zoomBehavior;
    },
    [transitionTimeOrDefault],
  );

  const makeTransition = useCallback(
    (element: HTMLDivElement) =>
      d3.select(element).transition().duration(transitionTimeOrDefault),
    [transitionTimeOrDefault],
  );

  const changeZoom = useCallback(
    (event: React.MouseEvent, factor: number) => {
      if (canvasRef.current === undefined || zoomRef.current === undefined) {
        return;
      }
      const zoomTransition = makeTransition(canvasRef.current);
      const center: [number, number] = [0, 0];
      zoomRef.current.scaleBy(zoomTransition, factor, center);
      event.preventDefault();
      event.stopPropagation();
    },
    [makeTransition],
  );

  const fitZoom = useCallback(
    (event: React.MouseEvent) => {
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
          (canvasWidth - fitPaddingOrDefault) / width,
          (canvasHeight - fitPaddingOrDefault) / height,
        );
        const zoomTransition = makeTransition(canvasRef.current);
        zoomRef.current.transform(
          zoomTransition,
          d3.zoomIdentity.scale(factor),
        );
      }
      event.preventDefault();
      event.stopPropagation();
    },
    [fitPaddingOrDefault, makeTransition],
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
          {children}
        </Box>
      </Box>
      <ZoomButtons changeZoom={changeZoom} fitZoom={fitZoom} />
    </Box>
  );
}

ZoomCanvas.defaultProps = {
  children: undefined,
  fitPadding: 64,
  transitionTime: 250,
};
