import { StateEffect, StateField, TransactionSpec } from '@codemirror/state';
import { EditorView, Decoration, DecorationSet } from '@codemirror/view';

const setSemanticHighlightingEffect = StateEffect.define<DecorationSet>();

export function setSemanticHighlighting(decorations: DecorationSet): TransactionSpec {
  return {
    effects: [
      setSemanticHighlightingEffect.of(decorations),
    ],
  };
}

export const semanticHighlighting = StateField.define<DecorationSet>({
  create() {
    return Decoration.none;
  },
  update(currentDecorations, transaction) {
    let newDecorations: DecorationSet | null = null;
    transaction.effects.forEach((effect) => {
      if (effect.is(setSemanticHighlightingEffect)) {
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
