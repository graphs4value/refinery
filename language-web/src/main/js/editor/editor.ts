import 'codemirror/addon/selection/active-line';
import 'mode-problem';
import { Controlled } from 'react-codemirror2';
import { createServices, removeServices } from 'xtext/xtext-codemirror';

export interface IEditorChunk {
  CodeMirror: typeof Controlled;

  createServices: typeof createServices;

  removeServices: typeof removeServices;
}

export const editorChunk: IEditorChunk = {
  CodeMirror: Controlled,
  createServices,
  removeServices,
};
