/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { EditorSelection } from '@codemirror/state';
import {
  type EditorView,
  type PluginValue,
  ViewPlugin,
} from '@codemirror/view';
import { reaction } from 'mobx';

import type EditorStore from './EditorStore';
import { getDiagnostics } from './exposeDiagnostics';
import findOccurrences from './findOccurrences';

export const HOLDER_CLASS = 'cm-scroller-holder';
export const SPACER_CLASS = 'cm-scroller-spacer';
export const TRACK_CLASS = 'cm-scroller-track';
export const THUMB_CLASS = 'cm-scroller-thumb';
export const THUMB_ACTIVE_CLASS = 'active';
export const GUTTER_DECORATION_CLASS = 'cm-scroller-gutter-decoration';
export const TOP_DECORATION_CLASS = 'cm-scroller-top-decoration';
export const ANNOTATION_SELECTION_CLASS = 'cm-scroller-selection';
export const ANNOTATION_DIAGNOSTIC_CLASS = 'cm-scroller-diagnostic';
export const ANNOTATION_OCCURRENCE_CLASS = 'cm-scroller-occurrence';
export const SHADOW_WIDTH = 10;
export const SCROLLBAR_WIDTH = 12;
export const ANNOTATION_WIDTH = SCROLLBAR_WIDTH / 2;
export const MIN_ANNOTATION_HEIGHT = 1;

function createScrollbar(
  holder: HTMLElement,
  direction: 'x' | 'y',
  touchCallback: (offsetX: number, offsetY: number) => void,
  moveCallback: (movementX: number, movementY: number) => void,
): { track: HTMLElement; thumb: HTMLElement } {
  const track = holder.ownerDocument.createElement('div');
  track.className = `${TRACK_CLASS} ${TRACK_CLASS}-${direction}`;
  holder.appendChild(track);

  const thumb = holder.ownerDocument.createElement('div');
  thumb.className = `${THUMB_CLASS} ${THUMB_CLASS}-${direction}`;
  track.appendChild(thumb);

  let pointerId: number | undefined;
  track.addEventListener('pointerdown', (event) => {
    if (pointerId !== undefined) {
      event.preventDefault();
      return;
    }
    ({ pointerId } = event);
    thumb.classList.add(THUMB_ACTIVE_CLASS);
    if (event.target === thumb) {
      // Prevent implicit pointer capture on mobile.
      thumb.releasePointerCapture(pointerId);
    } else {
      touchCallback(event.offsetX, event.offsetY);
    }
    track.setPointerCapture(pointerId);
  });

  track.addEventListener('pointermove', (event) => {
    if (event.pointerId !== pointerId) {
      return;
    }
    moveCallback(event.movementX, event.movementY);
    event.preventDefault();
  });

  function scrollEnd(event: PointerEvent) {
    if (event.pointerId !== pointerId) {
      return;
    }
    pointerId = undefined;
    thumb.classList.remove(THUMB_ACTIVE_CLASS);
  }

  track.addEventListener('pointerup', scrollEnd, { passive: true });
  track.addEventListener('pointercancel', scrollEnd, { passive: true });

  return { track, thumb };
}

function rebuildAnnotations(
  view: EditorView,
  scrollHeight: number,
  trackYHeight: number,
  holder: HTMLElement,
  annotations: HTMLDivElement[],
) {
  const { state } = view;
  const overlayAnnotationsHeight =
    (view.contentHeight / scrollHeight) * trackYHeight;
  const lineHeight = overlayAnnotationsHeight / state.doc.lines;

  let i = 0;

  function getOrCreateAnnotation(from: number, to?: number): HTMLDivElement {
    const startLine = state.doc.lineAt(from).number;
    const endLine = to === undefined ? startLine : state.doc.lineAt(to).number;
    const top = (startLine - 1) * lineHeight;
    const height = Math.max(
      MIN_ANNOTATION_HEIGHT,
      Math.max(1, endLine - startLine) * lineHeight,
    );

    let annotation: HTMLDivElement | undefined;
    if (i < annotations.length) {
      annotation = annotations[i];
    }
    if (annotation === undefined) {
      annotation = holder.ownerDocument.createElement('div');
      annotations.push(annotation);
      holder.appendChild(annotation);
    }
    i += 1;

    annotation.style.top = `${top}px`;
    annotation.style.height = `${height}px`;

    return annotation;
  }

  state.selection.ranges.forEach(({ head }) => {
    const selectionAnnotation = getOrCreateAnnotation(head);
    selectionAnnotation.className = ANNOTATION_SELECTION_CLASS;
    selectionAnnotation.style.width = `${SCROLLBAR_WIDTH}px`;
  });

  const diagnosticsIter = getDiagnostics(state).iter();
  while (diagnosticsIter.value !== null) {
    const diagnosticAnnotation = getOrCreateAnnotation(
      diagnosticsIter.from,
      diagnosticsIter.to,
    );
    diagnosticAnnotation.className = `${ANNOTATION_DIAGNOSTIC_CLASS} ${ANNOTATION_DIAGNOSTIC_CLASS}-${diagnosticsIter.value.severity}`;
    diagnosticAnnotation.style.width = `${ANNOTATION_WIDTH}px`;
    diagnosticsIter.next();
  }

  const occurrences = view.state.field(findOccurrences);
  const occurrencesIter = occurrences.iter();
  while (occurrencesIter.value !== null) {
    const occurrenceAnnotation = getOrCreateAnnotation(
      occurrencesIter.from,
      occurrencesIter.to,
    );
    occurrenceAnnotation.className = ANNOTATION_OCCURRENCE_CLASS;
    occurrenceAnnotation.style.width = `${ANNOTATION_WIDTH}px`;
    occurrenceAnnotation.style.right = `${ANNOTATION_WIDTH}px`;
    occurrencesIter.next();
  }

  annotations
    .splice(i)
    .forEach((staleAnnotation) => holder.removeChild(staleAnnotation));
}

export default function scrollbarViewPlugin(
  editorStore: EditorStore,
): ViewPlugin<PluginValue> {
  return ViewPlugin.define((view) => {
    const { scrollDOM } = view;
    const { ownerDocument, parentElement: parentDOM } = scrollDOM;
    if (parentDOM === null) {
      return {};
    }

    const holder = ownerDocument.createElement('div');
    holder.className = HOLDER_CLASS;
    parentDOM.replaceChild(holder, scrollDOM);
    holder.appendChild(scrollDOM);

    const spacer = ownerDocument.createElement('div');
    spacer.className = SPACER_CLASS;
    scrollDOM.insertBefore(spacer, scrollDOM.firstChild);

    let gutterWidth = 0;

    scrollDOM.addEventListener('click', (event) => {
      const scrollX = scrollDOM.scrollLeft + event.offsetX;
      const scrollY = scrollDOM.scrollTop + event.offsetY;
      if (scrollX > gutterWidth && scrollY > view.contentHeight) {
        event.preventDefault();
        view.focus();
        editorStore.dispatch({
          scrollIntoView: true,
          selection: EditorSelection.create([
            EditorSelection.cursor(view.state.doc.length),
          ]),
        });
      }
    });

    let factorY = 1;
    let factorX = 1;

    const { track: trackY, thumb: thumbY } = createScrollbar(
      holder,
      'y',
      (_offsetX, offsetY) => {
        const scaledOffset = offsetY / factorY;
        const { height: scrollerHeight } = scrollDOM.getBoundingClientRect();
        const target = Math.max(0, scaledOffset - scrollerHeight / 2);
        scrollDOM.scrollTo({ top: target });
      },
      (_movementX, movementY) => {
        scrollDOM.scrollBy({ top: movementY / factorY });
      },
    );

    const { track: trackX, thumb: thumbX } = createScrollbar(
      holder,
      'x',
      (offsetX) => {
        const scaledOffset = offsetX / factorX;
        const { width: scrollerWidth } = scrollDOM.getBoundingClientRect();
        const target = Math.max(0, scaledOffset - scrollerWidth / 2);
        scrollDOM.scrollTo({ left: target });
      },
      (movementX) => {
        scrollDOM.scrollBy({ left: movementX / factorX });
      },
    );

    const gutterDecoration = ownerDocument.createElement('div');
    gutterDecoration.className = GUTTER_DECORATION_CLASS;
    holder.appendChild(gutterDecoration);

    const topDecoration = ownerDocument.createElement('div');
    topDecoration.className = TOP_DECORATION_CLASS;
    holder.appendChild(topDecoration);

    const disposePanelReaction = reaction(
      () => editorStore.searchPanel.state,
      (panelOpen) => {
        topDecoration.style.display = panelOpen ? 'none' : 'block';
      },
      { fireImmediately: true },
    );

    let gutters: Element | undefined;

    let firstRun = true;
    let firstRunTimeout: number | undefined;
    let requested = false;
    let rebuildRequested = false;

    const annotations: HTMLDivElement[] = [];

    let observer: ResizeObserver | undefined;

    function update() {
      requested = false;

      if (gutters === undefined) {
        gutters = scrollDOM.querySelector('.cm-gutters') ?? undefined;
        if (gutters !== undefined && observer !== undefined) {
          observer.observe(gutters);
        }
      }

      const { height: scrollerHeight, width: scrollerWidth } =
        scrollDOM.getBoundingClientRect();
      const { scrollTop, scrollLeft, scrollWidth } = scrollDOM;
      const scrollHeight =
        view.contentHeight + scrollerHeight - view.defaultLineHeight;
      if (firstRun) {
        if (firstRunTimeout !== undefined) {
          clearTimeout(firstRunTimeout);
        }
        firstRunTimeout = setTimeout(() => {
          spacer.style.minHeight = `${scrollHeight}px`;
          firstRun = false;
        }, 0);
      } else {
        spacer.style.minHeight = `${scrollHeight}px`;
      }
      gutterWidth = gutters?.clientWidth ?? 0;
      let trackYHeight = scrollerHeight;

      // Prevent spurious horizontal scrollbar by rounding up to the nearest pixel.
      if (scrollWidth > Math.ceil(scrollerWidth)) {
        // Leave space for horizontal scrollbar.
        trackYHeight -= SCROLLBAR_WIDTH;
        // Alwalys leave space for annotation in the vertical scrollbar.
        const trackXWidth = scrollerWidth - gutterWidth - SCROLLBAR_WIDTH;
        const thumbWidth = trackXWidth * (scrollerWidth / scrollWidth);
        factorX = (trackXWidth - thumbWidth) / (scrollWidth - scrollerWidth);
        trackY.style.bottom = `${SCROLLBAR_WIDTH}px`;
        trackX.style.display = 'block';
        trackX.style.left = `${gutterWidth}px`;
        thumbX.style.width = `${thumbWidth}px`;
        thumbX.style.left = `${scrollLeft * factorX}px`;
        scrollDOM.style.overflowX = 'scroll';
      } else {
        trackY.style.bottom = '0px';
        trackX.style.display = 'none';
        scrollDOM.style.overflowX = 'hidden';
      }

      const thumbHeight = trackYHeight * (scrollerHeight / scrollHeight);
      factorY = (trackYHeight - thumbHeight) / (scrollHeight - scrollerHeight);
      thumbY.style.display = 'block';
      thumbY.style.height = `${thumbHeight}px`;
      thumbY.style.top = `${scrollTop * factorY}px`;

      gutterDecoration.style.left = `${gutterWidth}px`;
      gutterDecoration.style.width = `${Math.max(
        0,
        Math.min(scrollLeft, SHADOW_WIDTH),
      )}px`;

      topDecoration.style.height = `${Math.max(
        0,
        Math.min(scrollTop, SHADOW_WIDTH),
      )}px`;

      if (rebuildRequested) {
        rebuildAnnotations(
          view,
          scrollHeight,
          trackYHeight,
          holder,
          annotations,
        );
        rebuildRequested = false;
      }
    }

    function requestUpdate() {
      if (!requested) {
        requested = true;
        view.requestMeasure({ read: update });
      }
    }

    function requestRebuild() {
      requestUpdate();
      rebuildRequested = true;
    }

    observer = new ResizeObserver(requestRebuild);
    observer.observe(holder);

    scrollDOM.addEventListener('scroll', requestUpdate);

    requestRebuild();

    return {
      update: requestRebuild,
      destroy() {
        disposePanelReaction();
        observer?.disconnect();
        scrollDOM.removeEventListener('scroll', requestUpdate);
        parentDOM.replaceChild(holder, holder);
      },
    };
  });
}
