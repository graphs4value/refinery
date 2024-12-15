/*
 * Copyright (C) 2018-2021 by Marijn Haverbeke <marijn@haverbeke.berlin> and others
 * Copyright (C) 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 *
 * This file is based on
 * https://github.com/codemirror/view/blob/32aa0e88e9053bc867731c5057c30565b251ea26/src/rectangular-selection.ts#L89-L133
 * but it was modified to support a combination of Shift and Alt modifiers.
 */

import type { Extension } from '@codemirror/state';
import { EditorView, ViewPlugin } from '@codemirror/view';

const showCrosshair = { style: 'cursor: crosshair' };

/// Returns an extension that turns the pointer cursor into a
/// crosshair when Shift and Alt are held down together.
/// Can serve as a visual hint that rectangular selection is
/// going to happen when paired with
/// [`rectangularSelection`](#view.rectangularSelection).
export default function crosshairCursor(): Extension {
  const plugin = ViewPlugin.fromClass(
    class {
      private shiftDown = false;

      private altDown = false;

      get showCrosshair(): boolean {
        return this.shiftDown && this.altDown;
      }

      constructor(private readonly view: EditorView) {}

      setShift(shiftDown: boolean) {
        const oldValue = this.showCrosshair;
        this.shiftDown = shiftDown;
        if (oldValue !== this.showCrosshair) {
          this.view.update([]);
        }
      }

      setAlt(altDown: boolean) {
        const oldValue = this.showCrosshair;
        this.altDown = altDown;
        if (oldValue !== this.showCrosshair) {
          this.view.update([]);
        }
      }
    },
    {
      eventObservers: {
        keydown(e) {
          if (e.key === 'Shift' || e.shiftKey) {
            this.setShift(true);
          }
          if (e.key === 'Alt' || e.altKey) {
            this.setAlt(true);
          }
        },
        keyup(e) {
          if (e.key === 'Shift' || !e.shiftKey) {
            this.setShift(false);
          }
          if (e.key === 'Alt' || !e.altKey) {
            this.setAlt(false);
          }
        },
        mousemove(e) {
          this.setShift(e.shiftKey);
          this.setAlt(e.altKey);
        },
      },
    },
  );
  return [
    plugin,
    EditorView.contentAttributes.of((view) =>
      view.plugin(plugin)?.showCrosshair ? showCrosshair : null,
    ),
  ];
}
