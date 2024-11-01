/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { syntaxTree } from '@codemirror/language';
import { Extension, Prec, RangeSetBuilder } from '@codemirror/state';
import {
  Decoration,
  Direction,
  type PluginValue,
  type DecorationSet,
  EditorView,
  ViewUpdate,
  ViewPlugin,
} from '@codemirror/view';
import { Tree } from '@lezer/common';
import { direction } from 'direction';

const isolateLTR = Decoration.mark({
  attributes: { style: 'direction: ltr; unicode-bidi: isolate;' },
  bidiIsolate: Direction.LTR,
});

const isolateRTL = Decoration.mark({
  attributes: { style: 'direction: rtl; unicode-bidi: isolate;' },
  bidiIsolate: Direction.RTL,
});

/**
 * Isolates the text direction of quoted IDs from the rest of the code in the editor,
 * allowing the use of Unicode bidi identifiers without disturbing the text direction
 * of the rest of the code.
 *
 * @param view The editor view.
 * @returns The computed `isolateLTR` decoration.
 * @see https://codemirror.net/examples/bidi/#bidi-isolation
 */
function computeBidiIsolates(view: EditorView): DecorationSet {
  const builder = new RangeSetBuilder<Decoration>();
  view.visibleRanges.forEach(({ from, to }) => {
    syntaxTree(view.state).iterate({
      from,
      to,
      enter(node) {
        if (node.name !== 'QuotedID' && node.name !== 'String') {
          return;
        }
        const textFrom = node.from + 1;
        const textTo = node.to - 1;
        if (textFrom >= textTo) {
          return;
        }
        const dir = direction(view.state.sliceDoc(textFrom, textTo));
        builder.add(textFrom, textTo, dir === 'rtl' ? isolateRTL : isolateLTR);
      },
    });
  });
  return builder.finish();
}

class BidiIsolatesPlugin implements PluginValue {
  isolates: DecorationSet;

  tree: Tree;

  constructor(view: EditorView) {
    this.isolates = computeBidiIsolates(view);
    this.tree = syntaxTree(view.state);
  }

  update(update: ViewUpdate) {
    if (
      update.docChanged ||
      update.viewportChanged ||
      syntaxTree(update.state) !== this.tree
    ) {
      this.isolates = computeBidiIsolates(update.view);
      this.tree = syntaxTree(update.state);
    }
  }
}

export default function bidiIsolatesExtension(): Extension {
  return ViewPlugin.fromClass(BidiIsolatesPlugin, {
    provide: (plugin) => {
      function access(view: EditorView) {
        return view?.plugin(plugin)?.isolates ?? Decoration.none;
      }
      return Prec.lowest([
        EditorView.decorations.of(access),
        EditorView.bidiIsolatedRanges.of(access),
      ]);
    },
  });
}
