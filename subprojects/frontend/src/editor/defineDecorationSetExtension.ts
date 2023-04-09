/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { StateEffect, StateField, TransactionSpec } from '@codemirror/state';
import { EditorView, Decoration, DecorationSet } from '@codemirror/view';

export type TransactionSpecFactory = (
  decorations: DecorationSet,
) => TransactionSpec;

export default function defineDecorationSetExtension(): [
  TransactionSpecFactory,
  StateField<DecorationSet>,
] {
  const setEffect = StateEffect.define<DecorationSet>();
  const stateField = StateField.define<DecorationSet>({
    create() {
      return Decoration.none;
    },
    update(currentDecorations, transaction) {
      let newDecorations: DecorationSet | null = null;
      transaction.effects.forEach((effect) => {
        if (effect.is(setEffect)) {
          newDecorations = effect.value;
        }
      });
      if (newDecorations === null) {
        if (transaction.docChanged) {
          return currentDecorations.map(transaction.changes);
        }
        return currentDecorations;
      }
      return newDecorations;
    },
    provide: (field) => EditorView.decorations.from(field),
  });

  function transactionSpecFactory(decorations: DecorationSet) {
    return {
      effects: [setEffect.of(decorations)],
    };
  }

  return [transactionSpecFactory, stateField];
}
