/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { RangeSet, type Extension } from '@codemirror/state';
import { Decoration, EditorView, ViewPlugin } from '@codemirror/view';

import findToken from '../xtext/findToken';

import type EditorStore from './EditorStore';
import defineDecorationSetExtension from './defineDecorationSetExtension';

const showPointer = { style: 'cursor: pointer' };

const [setGoToDefinitionDecorations, goToDefinitionDecorations] =
  defineDecorationSetExtension();

const decoration = Decoration.mark({
  attributes: { style: 'text-decoration: underline' },
});

export default function goToDefinition(store: EditorStore): Extension {
  const plugin = ViewPlugin.fromClass(
    class {
      state = false;

      hasToken = false;

      ctrlDown = false;

      metaDown = false;

      private x: number | undefined;

      private y: number | undefined;

      constructor(private readonly view: EditorView) {}

      setCtrl(ctrlDown: boolean) {
        this.ctrlDown = ctrlDown;
      }

      setMeta(metaDown: boolean) {
        this.metaDown = metaDown;
      }

      update() {
        this.updateDecoration();
      }

      updateDecoration(clientX?: number, clientY?: number) {
        const newState = this.ctrlDown || this.metaDown;
        let shouldUpdate = this.state !== newState;
        if (clientX !== undefined && this.x !== clientX) {
          this.x = clientX;
          shouldUpdate = true;
        }
        if (clientY !== undefined && this.y !== clientY) {
          this.y = clientY;
          shouldUpdate = true;
        }
        if (!shouldUpdate) {
          return;
        }
        if (!newState || this.x === undefined || this.y === undefined) {
          this.state = false;
          this.setEmpty();
          return;
        }
        this.state = true;
        const offset = this.view.posAtCoords({ x: this.x, y: this.y });
        if (offset === null) {
          this.setEmpty();
          return;
        }
        const token = findToken(offset, this.view.state);
        if (!token?.idenfitier) {
          this.setEmpty();
          return;
        }
        this.hasToken = true;
        this.view.dispatch(
          setGoToDefinitionDecorations(
            RangeSet.of([
              {
                from: token.from,
                to: token.to,
                value: decoration,
              },
            ]),
          ),
        );
      }

      private setEmpty() {
        this.hasToken = false;
        this.view.dispatch(
          setGoToDefinitionDecorations(RangeSet.empty as RangeSet<Decoration>),
        );
      }

      goToDefinition(x: number, y: number) {
        const offset = this.view.posAtCoords({ x, y });
        if (offset !== null) {
          store.goToDefinition(offset);
        }
      }
    },
    {
      eventObservers: {
        keydown(e) {
          if (e.key === 'Control' || e.ctrlKey) {
            this.ctrlDown = true;
          }
          if (e.key === 'Meta' || e.metaKey) {
            this.metaDown = true;
          }
          this.updateDecoration();
        },
        keyup(e) {
          if (e.key === 'Control' || !e.ctrlKey) {
            this.ctrlDown = false;
          }
          if (e.key === 'Meta' || !e.metaKey) {
            this.metaDown = false;
          }
          this.updateDecoration();
        },
        mousemove(e) {
          this.ctrlDown = e.ctrlKey;
          this.metaDown = e.metaKey;
          this.updateDecoration(e.clientX, e.clientY);
        },
      },
      eventHandlers: {
        mousedown(e) {
          if (!this.state) {
            return false;
          }
          // Prevent normal selection when the go-to-definition function is active.
          e.preventDefault();
          return true;
        },
        click(e) {
          if (!this.state) {
            return false;
          }
          this.goToDefinition(e.clientX, e.clientY);
          e.preventDefault();
          return true;
        },
      },
    },
  );
  return [
    plugin,
    goToDefinitionDecorations,
    EditorView.contentAttributes.of((view) =>
      view.plugin(plugin)?.hasToken ? showPointer : null,
    ),
  ];
}
