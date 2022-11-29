import { EditorSelection } from '@codemirror/state';
import { type PluginValue, ViewPlugin } from '@codemirror/view';
import { reaction } from 'mobx';

import type EditorStore from './EditorStore';
import { getDiagnostics } from './exposeDiagnostics';
import findOccurrences from './findOccurrences';

export const HOLDER_CLASS = 'cm-scroller-holder';
export const SPACER_CLASS = 'cm-scroller-spacer';
export const THUMB_CLASS = 'cm-scroller-thumb';
export const THUMB_Y_CLASS = 'cm-scroller-thumb-y';
export const THUMB_X_CLASS = 'cm-scroller-thumb-x';
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

function handleDrag(
  element: HTMLElement,
  callback: (movementX: number, movementY: number) => void,
) {
  let pointerId: number | undefined;
  element.addEventListener('pointerdown', (event) => {
    if (pointerId === undefined) {
      ({ pointerId } = event);
      element.setPointerCapture(pointerId);
      element.classList.add(THUMB_ACTIVE_CLASS);
    } else {
      event.preventDefault();
      // Avoid implicit pointer capture, see https://w3c.github.io/pointerevents/#dfn-implicit-pointer-capture
      element.releasePointerCapture(event.pointerId);
    }
  });

  element.addEventListener('pointermove', (event) => {
    if (event.pointerId !== pointerId) {
      return;
    }
    callback(event.movementX, event.movementY);
    event.preventDefault();
  });

  function scrollEnd(event: PointerEvent) {
    if (event.pointerId !== pointerId) {
      return;
    }
    pointerId = undefined;
    element.classList.remove(THUMB_ACTIVE_CLASS);
  }

  element.addEventListener('pointerup', scrollEnd, { passive: true });
  element.addEventListener('pointercancel', scrollEnd, { passive: true });
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

    const thumbY = ownerDocument.createElement('div');
    thumbY.className = `${THUMB_CLASS} ${THUMB_Y_CLASS}`;
    handleDrag(thumbY, (_movementX, movementY) =>
      scrollDOM.scrollBy({ top: movementY / factorY }),
    );
    holder.appendChild(thumbY);

    const thumbX = ownerDocument.createElement('div');
    thumbX.className = `${THUMB_CLASS} ${THUMB_X_CLASS}`;
    handleDrag(thumbX, (movementX) =>
      scrollDOM.scrollBy({ left: movementX / factorX }),
    );
    holder.appendChild(thumbX);

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

    let requested = false;
    let rebuildRequested = false;

    const annotations: HTMLDivElement[] = [];

    function rebuildAnnotations(scrollHeight: number, trackYHeight: number) {
      const { state } = view;
      const overlayAnnotationsHeight =
        (view.contentHeight / scrollHeight) * trackYHeight;
      const lineHeight = overlayAnnotationsHeight / state.doc.lines;

      let i = 0;

      function getOrCreateAnnotation(
        from: number,
        to?: number,
      ): HTMLDivElement {
        const startLine = state.doc.lineAt(from).number;
        const endLine =
          to === undefined ? startLine : state.doc.lineAt(to).number;
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
          annotation = ownerDocument.createElement('div');
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
      spacer.style.minHeight = `${scrollHeight}px`;
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
        thumbX.style.display = 'block';
        thumbX.style.height = `${SCROLLBAR_WIDTH}px`;
        thumbX.style.width = `${thumbWidth}px`;
        thumbX.style.left = `${gutterWidth + scrollLeft * factorX}px`;
        scrollDOM.style.overflowX = 'scroll';
      } else {
        thumbX.style.display = 'none';
        scrollDOM.style.overflowX = 'hidden';
      }

      const thumbHeight = trackYHeight * (scrollerHeight / scrollHeight);
      factorY = (trackYHeight - thumbHeight) / (scrollHeight - scrollerHeight);
      thumbY.style.display = 'block';
      thumbY.style.height = `${thumbHeight}px`;
      thumbY.style.width = `${SCROLLBAR_WIDTH}px`;
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
        rebuildAnnotations(scrollHeight, trackYHeight);
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
