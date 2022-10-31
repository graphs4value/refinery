import { type PluginValue, ViewPlugin } from '@codemirror/view';
import { reaction } from 'mobx';

import type EditorStore from './EditorStore';

export const HOLDER_CLASS = 'cm-scroller-holder';
export const THUMB_CLASS = 'cm-scroller-thumb';
export const THUMB_Y_CLASS = 'cm-scroller-thumb-y';
export const THUMB_X_CLASS = 'cm-scroller-thumb-x';
export const THUMB_ACTIVE_CLASS = 'active';
export const GUTTER_DECORATION_CLASS = 'cm-scroller-gutter-decoration';
export const TOP_DECORATION_CLASS = 'cm-scroller-top-decoration';
export const SHADOW_WIDTH = 10;
export const SCROLLBAR_WIDTH = 12;

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

    let factorY = 1;
    let factorX = 1;

    const thumbY = ownerDocument.createElement('div');
    thumbY.className = `${THUMB_CLASS} ${THUMB_Y_CLASS}`;
    const scrollY = (event: MouseEvent) => {
      scrollDOM.scrollBy({ top: event.movementY / factorY });
      event.preventDefault();
    };
    const stopScrollY = () => {
      thumbY.classList.remove(THUMB_ACTIVE_CLASS);
      window.removeEventListener('mousemove', scrollY);
      window.removeEventListener('mouseup', stopScrollY);
    };
    thumbY.addEventListener(
      'mousedown',
      () => {
        thumbY.classList.add(THUMB_ACTIVE_CLASS);
        window.addEventListener('mousemove', scrollY);
        window.addEventListener('mouseup', stopScrollY, { passive: true });
      },
      { passive: true },
    );
    holder.appendChild(thumbY);

    const thumbX = ownerDocument.createElement('div');
    thumbX.className = `${THUMB_CLASS} ${THUMB_X_CLASS}`;
    const scrollX = (event: MouseEvent) => {
      scrollDOM.scrollBy({ left: event.movementX / factorX });
    };
    const stopScrollX = () => {
      thumbX.classList.remove(THUMB_ACTIVE_CLASS);
      window.removeEventListener('mousemove', scrollX);
      window.removeEventListener('mouseup', stopScrollX);
    };
    thumbX.addEventListener(
      'mousedown',
      () => {
        thumbX.classList.add(THUMB_ACTIVE_CLASS);
        window.addEventListener('mousemove', scrollX);
        window.addEventListener('mouseup', stopScrollX, { passive: true });
      },
      { passive: true },
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

    let observer: ResizeObserver | undefined;
    let gutters: Element | undefined;

    let requested = false;

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
      const { scrollTop, scrollHeight, scrollLeft, scrollWidth } = scrollDOM;
      const gutterWidth = gutters?.clientWidth ?? 0;
      let trackYHeight = scrollerHeight;

      if (scrollWidth > scrollerWidth) {
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
      } else {
        thumbX.style.display = 'none';
      }

      if (scrollHeight > scrollerHeight) {
        const thumbHeight = trackYHeight * (scrollerHeight / scrollHeight);
        factorY =
          (trackYHeight - thumbHeight) / (scrollHeight - scrollerHeight);
        thumbY.style.display = 'block';
        thumbY.style.height = `${thumbHeight}px`;
        thumbY.style.width = `${SCROLLBAR_WIDTH}px`;
        thumbY.style.top = `${scrollTop * factorY}px`;
      } else {
        thumbY.style.display = 'none';
      }

      gutterDecoration.style.left = `${gutterWidth}px`;
      gutterDecoration.style.width = `${Math.max(
        0,
        Math.min(scrollLeft, SHADOW_WIDTH),
      )}px`;

      topDecoration.style.height = `${Math.max(
        0,
        Math.min(scrollTop, SHADOW_WIDTH),
      )}px`;
    }

    function requestUpdate() {
      if (!requested) {
        requested = true;
        view.requestMeasure({ read: update });
      }
    }

    observer = new ResizeObserver(requestUpdate);
    observer.observe(scrollDOM);

    scrollDOM.addEventListener('scroll', requestUpdate);

    requestUpdate();

    return {
      update: requestUpdate,
      destroy() {
        disposePanelReaction();
        observer?.disconnect();
        scrollDOM.removeEventListener('scroll', requestUpdate);
        parentDOM.replaceChild(scrollDOM, holder);
      },
    };
  });
}
