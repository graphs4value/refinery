import { type CSSObject, type Theme, styled } from '@mui/material/styles';
import React, { memo, useEffect, useState } from 'react';

const SHADOW_SIZE = 10;

const EditorAreaDecoration = styled('div')({
  position: 'absolute',
  pointerEvents: 'none',
});

function shadowTheme(
  origin: string,
  scaleX: boolean,
  scaleY: boolean,
): CSSObject {
  function radialGradient(opacity: number, scale: string): string {
    return `radial-gradient(
        farthest-side at ${origin},
        rgba(0, 0, 0, ${opacity}),
        rgba(0, 0, 0, 0)
      )
      ${origin} /
      ${scaleX ? scale : '100%'}
      ${scaleY ? scale : '100%'}
      no-repeat`;
  }

  return {
    background: `
      ${radialGradient(0.2, '40%')},
      ${radialGradient(0.14, '50%')},
      ${radialGradient(0.12, '100%')}
    `,
  };
}

function animateSize(
  theme: Theme,
  direction: 'height' | 'width',
  opacity: number,
): CSSObject {
  return {
    [direction]: opacity * SHADOW_SIZE,
    transition: theme.transitions.create(direction, {
      duration: theme.transitions.duration.shortest,
      easing: theme.transitions.easing.sharp,
    }),
  };
}

const TopDecoration = memo(
  styled(EditorAreaDecoration, {
    shouldForwardProp: (prop) => prop !== 'visible' && prop !== 'opacity',
  })<{
    visible: boolean;
    opacity: number;
  }>(({ theme, visible, opacity }) => ({
    display: visible ? 'block' : 'none',
    top: 0,
    left: 0,
    right: 0,
    ...shadowTheme('50% 0', false, true),
    ...animateSize(theme, 'height', opacity),
  })),
);

const GutterDecoration = memo(
  styled(EditorAreaDecoration, {
    shouldForwardProp: (prop) =>
      prop !== 'top' &&
      prop !== 'bottom' &&
      prop !== 'guttersWidth' &&
      prop !== 'opacity',
  })<{
    top: number;
    bottom: number;
    guttersWidth: number;
    opacity: number;
  }>(({ theme, top, bottom, guttersWidth, opacity }) => ({
    top,
    left: guttersWidth,
    bottom,
    ...shadowTheme('0 50%', true, false),
    ...animateSize(theme, 'width', opacity),
  })),
);

function convertToOpacity(scroll: number): number {
  return Math.max(0, Math.min(1, scroll / SHADOW_SIZE));
}

export default function EditorAreaDecorations({
  parent,
  scroller,
}: {
  parent: HTMLElement | undefined;
  scroller: HTMLElement | undefined;
}): JSX.Element {
  const [top, setTop] = useState(0);
  const [bottom, setBottom] = useState(0);
  const [guttersWidth, setGuttersWidth] = useState(0);
  const [topOpacity, setTopOpacity] = useState(0);
  const [gutterOpacity, setGutterOpacity] = useState(0);

  useEffect(() => {
    if (parent === undefined || scroller === undefined) {
      return () => {};
    }
    const gutters = scroller.querySelector('.cm-gutters');

    const updateBounds = () => {
      const parentRect = parent.getBoundingClientRect();
      const rect = scroller.getBoundingClientRect();
      setTop(rect.top - parentRect.top);
      setBottom(parentRect.bottom - rect.bottom);
      setGuttersWidth(gutters?.clientWidth ?? 0);
    };
    updateBounds();
    const resizeObserver = new ResizeObserver(updateBounds);
    resizeObserver.observe(scroller);
    if (gutters !== null) {
      resizeObserver.observe(gutters);
    }

    const updateScroll = () => {
      setTopOpacity(convertToOpacity(scroller.scrollTop));
      setGutterOpacity(convertToOpacity(scroller.scrollLeft));
    };
    updateScroll();
    scroller.addEventListener('scroll', updateScroll);

    return () => {
      resizeObserver.disconnect();
      scroller.removeEventListener('scroll', updateScroll);
    };
  }, [parent, scroller, setTop]);

  return (
    <>
      <GutterDecoration
        top={top}
        bottom={bottom}
        guttersWidth={guttersWidth}
        opacity={gutterOpacity}
      />
      <TopDecoration visible={top === 0} opacity={topOpacity} />
    </>
  );
}
