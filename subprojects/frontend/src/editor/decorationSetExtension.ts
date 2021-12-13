import { StateEffect, StateField, TransactionSpec } from '@codemirror/state';
import { EditorView, Decoration, DecorationSet } from '@codemirror/view';

export type TransactionSpecFactory = (decorations: DecorationSet) => TransactionSpec;

export function decorationSetExtension(): [TransactionSpecFactory, StateField<DecorationSet>] {
  const setEffect = StateEffect.define<DecorationSet>();
  const field = StateField.define<DecorationSet>({
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
    provide: (f) => EditorView.decorations.from(f),
  });

  function transactionSpecFactory(decorations: DecorationSet) {
    return {
      effects: [
        setEffect.of(decorations),
      ],
    };
  }

  return [transactionSpecFactory, field];
}
