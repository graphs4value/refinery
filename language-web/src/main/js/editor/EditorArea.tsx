import { observer } from 'mobx-react-lite';
import React, { useRef } from 'react';

import { useRootStore } from '../RootStore';

export const EditorArea = observer(() => {
  const { editorStore } = useRootStore();
  const { CodeMirror } = editorStore.chunk || {};
  const fallbackTextarea = useRef<HTMLTextAreaElement>(null);

  if (!CodeMirror) {
    return (
      <textarea
        value={editorStore.value}
        onChange={(e) => editorStore.updateValue(e.target.value)}
        ref={fallbackTextarea}
        className={`problem-fallback-editor cm-s-${editorStore.codeMirrorTheme}`}
      >
      </textarea>
    );
  }

  const textarea = fallbackTextarea.current;
  if (textarea) {
    editorStore.setInitialSelection(
      textarea.selectionStart,
      textarea.selectionEnd,
      document.activeElement === textarea,
    );
  }

  return (
    <CodeMirror
      value={editorStore.value}
      options={editorStore.codeMirrorOptions}
      editorDidMount={(editor) => editorStore.editorDidMount(editor)}
      editorWillUnmount={() => editorStore.editorWillUnmount()}
      onBeforeChange={(_editor, _data, value) => editorStore.updateValue(value)}
      onChange={() => editorStore.reportChanged()}
    />
  );
});
