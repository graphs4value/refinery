/**
 * @file CodeMirror plugin to highlight indentation
 *
 * This file is based on the
 * [@replit/codemirror-indentation-markers](https://github.com/replit/codemirror-indentation-markers)
 * package, which is available under the
 * [MIT License](https://github.com/replit/codemirror-indentation-markers/blob/543cc508ca5cef5d8350af23973eb1425e31525c/LICENSE).
 *
 * The highlighting heuristics were adjusted to make them more suitable
 * for logic programming.
 *
 * @see https://github.com/replit/codemirror-indentation-markers/blob/543cc508ca5cef5d8350af23973eb1425e31525c/src/index.ts
 */

import { getIndentUnit } from '@codemirror/language';
import { Text, RangeSet, EditorState } from '@codemirror/state';
import {
  ViewPlugin,
  Decoration,
  EditorView,
  WidgetType,
  PluginValue,
} from '@codemirror/view';

export const INDENTATION_MARKER_CLASS = 'cm-indentation-marker';

export const INDENTATION_MARKER_ACTIVE_CLASS = 'active';

const indentationMark = Decoration.mark({
  class: INDENTATION_MARKER_CLASS,
  tagName: 'span',
});

const activeIndentationMark = Decoration.mark({
  class: `${INDENTATION_MARKER_CLASS} ${INDENTATION_MARKER_ACTIVE_CLASS}`,
  tagName: 'span',
});

/**
 * Widget used to simulate N indentation markers on empty lines.
 */
class IndentationWidget extends WidgetType {
  constructor(
    readonly numIndent: number,
    readonly indentSize: number,
    readonly activeIndent?: number,
  ) {
    super();
  }

  override eq(other: IndentationWidget) {
    return (
      this.numIndent === other.numIndent &&
      this.indentSize === other.indentSize &&
      this.activeIndent === other.activeIndent
    );
  }

  override toDOM(view: EditorView) {
    const indentSize = getIndentUnit(view.state);

    const wrapper = document.createElement('span');
    wrapper.style.top = '0';
    wrapper.style.left = '4px';
    wrapper.style.position = 'absolute';
    wrapper.style.pointerEvents = 'none';

    for (let indent = 0; indent < this.numIndent; indent += 1) {
      const element = document.createElement('span');
      element.className = INDENTATION_MARKER_CLASS;
      element.classList.toggle(
        INDENTATION_MARKER_ACTIVE_CLASS,
        indent === this.activeIndent,
      );
      element.innerHTML = ' '.repeat(indentSize);
      wrapper.appendChild(element);
    }

    return wrapper;
  }
}

/**
 * Returns the number of indentation markers a non-empty line should have
 * based on the text in the line and the size of the indent.
 */
function getNumIndentMarkersForNonEmptyLine(
  text: string,
  indentSize: number,
  onIndentMarker?: (pos: number) => void,
) {
  let numIndents = 0;
  let numConsecutiveSpaces = 0;
  let prevChar: string | null = null;

  for (let char = 0; char < text.length; char += 1) {
    // Bail if we encounter a non-whitespace character
    if (text[char] !== ' ' && text[char] !== '\t') {
      // We still increment the indentation level if we would
      // have added a marker here had this been a space or tab.
      if (numConsecutiveSpaces % indentSize === 0 && char !== 0) {
        numIndents += 1;
      }

      return numIndents;
    }

    // Every tab and N space has an indentation marker
    const shouldAddIndent =
      prevChar === '\t' || numConsecutiveSpaces % indentSize === 0;

    if (shouldAddIndent) {
      numIndents += 1;

      if (onIndentMarker) {
        onIndentMarker(char);
      }
    }

    if (text[char] === ' ') {
      numConsecutiveSpaces += 1;
    } else {
      numConsecutiveSpaces = 0;
    }

    prevChar = text[char];
  }

  return numIndents;
}

/**
 * Returns the number of indent markers an empty line should have
 * based on the number of indent markers of the previous
 * and next non-empty lines.
 */
function getNumIndentMarkersForEmptyLine(prev: number, next: number) {
  const min = Math.min(prev, next);
  const max = Math.max(prev, next);

  // If only one side is non-zero, we omit markers,
  // because in logic programming, a block often ends with an empty line.
  if (min === 0 && max > 0) {
    return 0;
  }

  // Else, default to the minimum of the two
  return min;
}

/**
 * Returns the next non-empty line and its indent level.
 */
function findNextNonEmptyLineAndIndentLevel(
  doc: Text,
  startLine: number,
  indentSize: number,
): [number, number] {
  const numLines = doc.lines;
  let lineNo = startLine;

  while (lineNo <= numLines) {
    const { text } = doc.line(lineNo);

    if (text.trim().length === 0) {
      lineNo += 1;
    } else {
      const indent = getNumIndentMarkersForNonEmptyLine(text, indentSize);
      return [lineNo, indent];
    }
  }

  // Reached the end of the doc
  return [numLines + 1, 0];
}

interface IndentationMarkerDesc {
  lineNumber: number;
  from: number;
  to: number;
  create(activeIndentIndex?: number): Decoration;
}

/**
 * Returns a range of lines with an active indent marker.
 */
function getLinesWithActiveIndentMarker(
  state: EditorState,
  indentMap: Map<number, number>,
): { start: number; end: number; activeIndent: number } {
  const currentLine = state.doc.lineAt(state.selection.main.head);
  const currentIndent = indentMap.get(currentLine.number);
  const currentLineNo = currentLine.number;

  if (!currentIndent) {
    return { start: -1, end: -1, activeIndent: NaN };
  }

  let start: number;
  let end: number;

  for (start = currentLineNo; start >= 0; start -= 1) {
    const indent = indentMap.get(start - 1);
    if (!indent || indent < currentIndent) {
      break;
    }
  }

  for (end = currentLineNo; ; end += 1) {
    const indent = indentMap.get(end + 1);
    if (!indent || indent < currentIndent) {
      break;
    }
  }

  return { start, end, activeIndent: currentIndent };
}
/**
 * Adds indentation markers to all lines within view.
 */
function addIndentationMarkers(view: EditorView) {
  const indentSize = getIndentUnit(view.state);
  const indentSizeMap = new Map</* lineNumber */ number, number>();
  const decorations: Array<IndentationMarkerDesc> = [];

  view.visibleRanges.forEach(({ from, to }) => {
    let pos = from;

    let prevIndentMarkers = 0;
    let nextIndentMarkers = 0;
    let nextNonEmptyLine = 0;

    while (pos <= to) {
      const line = view.state.doc.lineAt(pos);
      const { text } = line;

      // If a line is empty, we match the indentation according
      // to a heuristic based on the indentations of the
      // previous and next non-empty lines.
      if (text.trim().length === 0) {
        // To retrieve the next non-empty indentation level,
        // we perform a lookahead and cache the result.
        if (nextNonEmptyLine < line.number) {
          const [nextLine, nextIndent] = findNextNonEmptyLineAndIndentLevel(
            view.state.doc,
            line.number + 1,
            indentSize,
          );

          nextNonEmptyLine = nextLine;
          nextIndentMarkers = nextIndent;
        }

        const numIndentMarkers = getNumIndentMarkersForEmptyLine(
          prevIndentMarkers,
          nextIndentMarkers,
        );

        // Add the indent widget and move on to next line
        indentSizeMap.set(line.number, numIndentMarkers);
        decorations.push({
          from: pos,
          to: pos,
          lineNumber: line.number,
          create: (activeIndentIndex) =>
            Decoration.widget({
              widget: new IndentationWidget(
                numIndentMarkers,
                indentSize,
                activeIndentIndex,
              ),
            }),
        });
      } else {
        const indices: Array<number> = [];

        prevIndentMarkers = getNumIndentMarkersForNonEmptyLine(
          text,
          indentSize,
          (char) => indices.push(char),
        );

        indentSizeMap.set(line.number, indices.length);
        decorations.push(
          ...indices.map(
            (char, i): IndentationMarkerDesc => ({
              from: line.from + char,
              to: line.from + char + 1,
              lineNumber: line.number,
              create: (activeIndentIndex) =>
                activeIndentIndex === i
                  ? activeIndentationMark
                  : indentationMark,
            }),
          ),
        );
      }

      // Move on to the next line
      pos = line.to + 1;
    }
  });

  const activeBlockRange = getLinesWithActiveIndentMarker(
    view.state,
    indentSizeMap,
  );

  return RangeSet.of<Decoration>(
    Array.from(decorations).map(({ lineNumber, from, to, create }) => {
      const activeIndent =
        lineNumber >= activeBlockRange.start &&
        lineNumber <= activeBlockRange.end
          ? activeBlockRange.activeIndent - 1
          : undefined;

      return { from, to, value: create(activeIndent) };
    }),
    true,
  );
}

export default function indentationMarkerViewPlugin() {
  return ViewPlugin.define<PluginValue & { decorations: RangeSet<Decoration> }>(
    (view) => ({
      decorations: addIndentationMarkers(view),
      update(update) {
        if (
          update.docChanged ||
          update.viewportChanged ||
          update.selectionSet
        ) {
          this.decorations = addIndentationMarkers(update.view);
        }
      },
    }),
    {
      decorations: (v) => v.decorations,
    },
  );
}
