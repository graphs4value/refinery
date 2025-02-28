/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { EditorSelection, type Extension } from '@codemirror/state';
import {
  EditorView,
  ViewPlugin,
  ViewUpdate,
  type PluginValue,
} from '@codemirror/view';

import { getDiagnostics } from './exposeDiagnostics';
import findOccurrences from './findOccurrences';
import normalizeWheel from './normalizeWheel';

const scrollbarWidth = 12;

const shadowWidth = 10;

class ScrollbarsPlugin implements PluginValue {
  private readonly editorDOM: HTMLElement;

  private readonly scrollDOM: HTMLElement;

  private readonly gutterDOM: HTMLElement;

  private readonly contentDOM: HTMLElement;

  private readonly rightTrack: HTMLElement;

  private readonly rightThumb: HTMLElement;

  private readonly annotationsDOM: HTMLElement;

  private readonly topShadow: HTMLElement;

  private readonly leftShadow: HTMLElement;

  private readonly bottomTrack: HTMLElement;

  private readonly bottomThumb: HTMLElement;

  private readonly layoutListener: () => void;

  private readonly resizeObserver: ResizeObserver;

  private needsRightTrack = false;

  private needsBottomTrack = false;

  private scrollingRight: number | undefined;

  private previousRight = 0;

  private scrollingBottom: number | undefined;

  private previousBottom = 0;

  private annotations: HTMLElement[] = [];

  private readonly overscrollClickListener: (event: MouseEvent) => void;

  private lastPaddingBottom = 0;

  constructor(private readonly view: EditorView) {
    this.editorDOM = view.dom;
    this.scrollDOM = view.scrollDOM;
    const { ownerDocument: document } = this.editorDOM;
    const gutterElement = this.scrollDOM.querySelector('.cm-gutters');
    if (gutterElement === null) {
      throw new Error('CodeMirror scrollDOM without gutter element');
    }
    this.gutterDOM = gutterElement as HTMLElement;
    this.contentDOM = view.contentDOM;
    this.rightTrack = document.createElement('div');
    this.rightTrack.classList.add('cm-track', 'cm-right-track');
    this.rightThumb = document.createElement('div');
    this.rightThumb.classList.add('cm-thumb', 'cm-right-thumb');
    this.rightTrack.appendChild(this.rightThumb);
    this.rightTrack.addEventListener('pointerdown', (event) => {
      if (event.button !== 0) {
        return;
      }
      const clickOffset = this.scaleRight(event.offsetY);
      const scrollPosition = Math.min(
        Math.max(0, clickOffset - this.scrollDOM.clientHeight / 2),
        this.scrollDOM.scrollHeight - this.scrollDOM.clientHeight,
      );
      this.scrollDOM.scrollTo({ top: scrollPosition });
      this.startScrollRight(event);
    });
    this.rightThumb.addEventListener('pointerdown', (event) => {
      if (event.button !== 0) {
        return;
      }
      event.stopPropagation();
      this.startScrollRight(event);
    });
    this.rightTrack.addEventListener('pointermove', (event) => {
      if (this.scrollingRight !== event.pointerId) {
        return;
      }
      // We can't use `movementY` here because of https://github.com/w3c/pointerlock/issues/100
      const scaledDelta = this.scaleRight(event.clientY - this.previousRight);
      this.previousRight = event.clientY;
      this.scrollDOM.scrollBy({ top: scaledDelta, behavior: 'instant' });
    });
    this.rightTrack.addEventListener('pointerup', (event) => {
      if (this.scrollingRight !== event.pointerId || event.button !== 0) {
        return;
      }
      this.rightThumb.classList.remove('cm-thumb-active');
      this.rightTrack.releasePointerCapture(event.pointerId);
      this.scrollingRight = undefined;
    });
    this.rightTrack.addEventListener(
      'wheel',
      (event) => {
        const { pixelX, pixelY } = normalizeWheel(event);
        this.scrollDOM.scrollBy({
          left: pixelX,
          top: pixelY,
          behavior: 'instant',
        });
      },
      { passive: true },
    );
    this.annotationsDOM = document.createElement('div');
    this.annotationsDOM.classList.add('cm-track-annotations');
    this.rightTrack.appendChild(this.annotationsDOM);
    this.editorDOM.appendChild(this.rightTrack);
    this.bottomTrack = document.createElement('div');
    this.bottomTrack.classList.add('cm-track', 'cm-bottom-track');
    this.bottomThumb = document.createElement('div');
    this.bottomThumb.classList.add('cm-thumb', 'cm-bottom-thumb');
    this.bottomTrack.appendChild(this.bottomThumb);
    this.bottomTrack.addEventListener('pointerdown', (event) => {
      if (event.button !== 0) {
        return;
      }
      const clickOffset = this.scaleBottom(event.offsetX);
      const scrollPosition = Math.min(
        Math.max(0, clickOffset - this.scrollDOM.clientWidth / 2),
        this.scrollDOM.scrollWidth - this.scrollDOM.clientWidth,
      );
      this.scrollDOM.scrollTo({ left: scrollPosition });
      this.startScrollBottom(event);
    });
    this.bottomThumb.addEventListener('pointerdown', (event) => {
      if (event.button !== 0) {
        return;
      }
      event.stopPropagation();
      this.startScrollBottom(event);
    });
    this.bottomTrack.addEventListener('pointermove', (event) => {
      if (this.scrollingBottom !== event.pointerId) {
        return;
      }
      // We can't use `movementX` here because of https://github.com/w3c/pointerlock/issues/100
      const scaledDelta = this.scaleBottom(event.clientX - this.previousBottom);
      this.previousBottom = event.clientX;
      this.scrollDOM.scrollBy({ left: scaledDelta, behavior: 'instant' });
    });
    this.bottomTrack.addEventListener('pointerup', (event) => {
      if (this.scrollingBottom !== event.pointerId || event.button !== 0) {
        return;
      }
      this.bottomThumb.classList.remove('cm-thumb-active');
      this.bottomTrack.releasePointerCapture(event.pointerId);
      this.scrollingBottom = undefined;
    });
    this.bottomTrack.addEventListener(
      'wheel',
      (event) => {
        const { pixelX, pixelY } = normalizeWheel(event);
        this.scrollDOM.scrollBy({
          // Swap scroll axes on the bottom scrollbar to allow scrolling with a single-wheel mouse.
          left: pixelY,
          top: pixelX,
          behavior: 'instant',
        });
      },
      { passive: true },
    );
    this.editorDOM.appendChild(this.bottomTrack);
    this.topShadow = document.createElement('div');
    this.topShadow.classList.add('cm-shadow', 'cm-top-shadow');
    this.editorDOM.appendChild(this.topShadow);
    this.leftShadow = document.createElement('div');
    this.leftShadow.classList.add('cm-shadow', 'cm-left-shadow');
    this.editorDOM.appendChild(this.leftShadow);
    this.overscrollClickListener = (event) => {
      const scrollX = this.scrollDOM.scrollLeft + event.offsetX;
      const scrollY = this.scrollDOM.scrollTop + event.offsetY;
      if (
        // This even may be triggered after a selection when CodeMirror virutalizes part of the lines.
        // Testing the event target makes sure that we don't accidentally scroll down to the last line
        // after the user completes a selection.
        event.target === this.scrollDOM &&
        scrollX > this.gutterDOM.offsetWidth &&
        scrollY > this.contentDOM.offsetHeight
      ) {
        event.preventDefault();
        this.view.focus();
        this.view.dispatch({
          scrollIntoView: true,
          selection: EditorSelection.create([
            EditorSelection.cursor(
              this.view.state.doc.line(this.view.state.doc.lines).from,
            ),
          ]),
        });
      }
    };
    this.scrollDOM.addEventListener('click', this.overscrollClickListener);
    this.layoutListener = () => this.updateLayout();
    this.view.scrollDOM.addEventListener('scroll', this.layoutListener);
    this.resizeObserver = new ResizeObserver(this.layoutListener);
    this.resizeObserver.observe(this.scrollDOM);
    this.resizeObserver.observe(this.gutterDOM);
    this.resizeObserver.observe(this.contentDOM);
  }

  private scaleRight(amount: number) {
    return (
      (amount / this.rightTrack.clientHeight) * this.scrollDOM.scrollHeight
    );
  }

  private scaleBottom(amount: number) {
    return (amount / this.bottomTrack.clientWidth) * this.scrollDOM.scrollWidth;
  }

  private startScrollRight(event: PointerEvent) {
    if (this.scrollingRight !== undefined) {
      return;
    }
    this.scrollingRight = event.pointerId;
    this.previousRight = event.clientY;
    this.rightTrack.setPointerCapture(event.pointerId);
    this.rightThumb.classList.add('cm-thumb-active');
  }

  private startScrollBottom(event: PointerEvent) {
    if (this.scrollingBottom !== undefined) {
      return;
    }
    this.scrollingBottom = event.pointerId;
    this.previousBottom = event.clientX;
    this.bottomTrack.setPointerCapture(event.pointerId);
    this.bottomThumb.classList.add('cm-thumb-active');
  }

  update({ state }: ViewUpdate): void {
    const { ownerDocument: document } = this.editorDOM;
    const { lines } = state.doc;
    let i = 0;
    const createAnnotation = (
      className: string,
      from: number,
      to?: number,
    ): void => {
      let annotation = this.annotations[i];
      if (annotation === undefined) {
        annotation = document.createElement('div');
        this.annotationsDOM.appendChild(annotation);
        this.annotations.push(annotation);
      }
      annotation.className = className;
      const fromLine = state.doc.lineAt(from).number - 1;
      const toLine =
        to === undefined ? fromLine : state.doc.lineAt(to).number - 1;
      annotation.style.top = `${(fromLine / lines) * 100}%`;
      annotation.style.height = `${((toLine - fromLine + 1) / lines) * 100}%`;
      i += 1;
    };
    state.selection.ranges.forEach(({ head }) =>
      createAnnotation(
        'cm-track-annotation cm-track-annotation-selection',
        head,
      ),
    );
    const diagnosticsIter = getDiagnostics(state).iter();
    while (diagnosticsIter.value !== null) {
      createAnnotation(
        `cm-track-annotation cm-track-annotation-diagnostic cm-track-annotation-diagnostic-${diagnosticsIter.value.severity}`,
        diagnosticsIter.from,
        diagnosticsIter.to,
      );
      diagnosticsIter.next();
    }
    const occurrencesIter = state.field(findOccurrences).iter();
    while (occurrencesIter.value !== null) {
      createAnnotation(
        'cm-track-annotation cm-track-annotation-occurrence',
        occurrencesIter.from,
        occurrencesIter.to,
      );
      occurrencesIter.next();
    }
    this.annotations
      .splice(i)
      .forEach((staleAnnotation) =>
        this.annotationsDOM.removeChild(staleAnnotation),
      );
  }

  docViewUpdate(): void {
    this.updateLayout();
  }

  private updateLayout(): void {
    this.view.requestMeasure({
      read: () => {
        const {
          scrollTop,
          scrollLeft,
          offsetTop,
          scrollWidth,
          scrollHeight,
          clientWidth,
          clientHeight,
        } = this.scrollDOM;
        const { offsetWidth: gutterWidth } = this.gutterDOM;
        const { offsetHeight: contentHeight } = this.contentDOM;
        const contentStyle = getComputedStyle(this.contentDOM);
        const lineHeight = parseFloat(contentStyle.lineHeight);
        const paddingBottom = Math.max(0, clientHeight - lineHeight);
        let bottom = 0;
        const bottomPanel = this.editorDOM.querySelector(
          ':scope > div.cm-panels-bottom',
        );
        if (bottomPanel !== null) {
          bottom = (bottomPanel as HTMLElement).offsetHeight;
        }
        return {
          scrollTop,
          scrollLeft,
          scrollHeight: scrollHeight - this.lastPaddingBottom + paddingBottom,
          scrollWidth: scrollWidth - gutterWidth,
          top: offsetTop,
          height: clientHeight,
          width: clientWidth - gutterWidth,
          bottom,
          gutterWidth,
          contentHeight,
          paddingBottom,
        };
      },
      write: (measure) => {
        if (this.lastPaddingBottom !== measure.paddingBottom) {
          this.contentDOM.style.marginBottom = `${measure.paddingBottom}px`;
          this.lastPaddingBottom = measure.paddingBottom;
        }
        const newNeedsRightTrack = measure.scrollHeight > measure.height;
        if (newNeedsRightTrack !== this.needsRightTrack) {
          const value = String(newNeedsRightTrack);
          this.editorDOM.setAttribute('data-needs-right-track', value);
          // Workaround since we can't apply selector to `.cm-editor` in a `@media` query.
          this.scrollDOM.setAttribute('data-needs-right-track', value);
          this.needsRightTrack = newNeedsRightTrack;
        }
        const newNeedsBottomTrack = measure.scrollWidth > measure.width;
        if (newNeedsBottomTrack !== this.needsBottomTrack) {
          const value = String(newNeedsBottomTrack);
          this.editorDOM.setAttribute('data-needs-bottom-track', value);
          // Workaround since we can't apply selector to `.cm-editor` in a `@media` query.
          this.scrollDOM.setAttribute('data-needs-bottom-track', value);
          this.needsBottomTrack = newNeedsBottomTrack;
        }
        if (this.needsBottomTrack) {
          this.bottomTrack.style.bottom = `${measure.bottom}px`;
          this.bottomTrack.style.left = `${measure.gutterWidth}px`;
          this.bottomThumb.style.width = `${(measure.width / measure.scrollWidth) * 100}%`;
          this.bottomThumb.style.left = `${(measure.scrollLeft / measure.scrollWidth) * 100}%`;
        }
        if (this.needsRightTrack) {
          this.rightTrack.style.top = `${measure.top}px`;
          this.rightTrack.style.bottom = `${measure.bottom}px`;
          this.rightThumb.style.height = `${(measure.height / measure.scrollHeight) * 100}%`;
          this.rightThumb.style.top = `${(measure.scrollTop / measure.scrollHeight) * 100}%`;
          this.annotationsDOM.style.height = `${(measure.contentHeight / measure.scrollHeight) * 100}%`;
        }
        this.topShadow.style.top = `${measure.top}px`;
        this.topShadow.style.height = `${Math.min(measure.scrollTop, shadowWidth)}px`;
        this.leftShadow.style.top = `${measure.top}px`;
        this.leftShadow.style.bottom = `${measure.bottom}px`;
        this.leftShadow.style.left = `${measure.gutterWidth}px`;
        this.leftShadow.style.width = `${Math.min(measure.scrollLeft, shadowWidth)}px`;
      },
      key: this,
    });
  }

  destroy(): void {
    this.scrollDOM.removeEventListener('click', this.overscrollClickListener);
    this.resizeObserver.disconnect();
    this.view.scrollDOM.removeEventListener('scroll', this.layoutListener);
    this.editorDOM.removeChild(this.rightTrack);
    this.editorDOM.removeChild(this.bottomTrack);
  }
}

const scrollbarsPlugin = ViewPlugin.fromClass(ScrollbarsPlugin);

function shadowTheme(
  origin: string,
  scaleX: boolean,
  scaleY: boolean,
): Record<string, string> {
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

const scrollbarsTheme = EditorView.baseTheme({
  '.cm-scroller': {
    overflow: 'scroll',
    overscrollBehavior: 'none',
    scrollbarWidth: 'none',
    '&::-webkit-scrollbar': {
      background: 'transparent',
      width: 0,
      height: 0,
    },
  },
  '.cm-content': {
    minHeight: 'auto',
    paddingTop: 0,
    paddingBottom: 0,
  },
  '.cm-track': {
    display: 'none',
    position: 'absolute',
    overflow: 'hidden',
    userSelect: 'none',
    touchAction: 'none',
  },
  '.cm-thumb': {
    position: 'absolute',
    zIndex: 300,
    userSelect: 'none',
    touchAction: 'none',
  },
  '.cm-right-track': {
    top: 0,
    bottom: 0,
    right: 0,
    width: `${scrollbarWidth}px`,
  },
  '.cm-right-thumb': {
    left: 0,
    width: `${scrollbarWidth}px`,
  },
  '&[data-needs-right-track="true"]': {
    '& .cm-right-track': {
      display: 'block',
    },
    '& .cm-bottom-track': {
      marginRight: `${scrollbarWidth}px`,
    },
  },
  '.cm-bottom-track': {
    bottom: 0,
    left: 0,
    right: 0,
    height: `${scrollbarWidth}px`,
    marginRight: `${scrollbarWidth}px`,
  },
  '.cm-bottom-thumb': {
    top: 0,
    height: `${scrollbarWidth}px`,
  },
  '&[data-needs-bottom-track="true"]': {
    '& .cm-bottom-track': {
      display: 'block',
    },
    '& .cm-right-track': {
      marginBottom: `${scrollbarWidth}px`,
    },
  },
  '.cm-shadow': {
    position: 'absolute',
  },
  '.cm-top-shadow': {
    top: 0,
    left: 0,
    right: 0,
    height: 0,
    ...shadowTheme('50% 0', false, true),
  },
  '.cm-left-shadow': {
    top: 0,
    bottom: 0,
    left: 0,
    width: 0,
    ...shadowTheme('0 50%', true, false),
  },
  '.cm-track-annotations': {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    maxHeight: '100%',
  },
  '.cm-track-annotation': {
    position: 'absolute',
    minHeight: '2px',
    pointerEvents: 'none',
  },
  '@media (prefers-reduced-transparency: reduce)': {
    '.cm-scroller[data-needs-right-track="true"]': {
      marginRight: `${scrollbarWidth}px`,
    },
    '.cm-scroller[data-needs-bottom-track="true"]': {
      marginBottom: `${scrollbarWidth}px`,
    },
  },
});

export default function scrollbarsExtension(): Extension {
  return [scrollbarsTheme, scrollbarsPlugin];
}
