import Portal from '@mui/material/Portal';
import { observer } from 'mobx-react-lite';

import type EditorStore from './EditorStore';
import SearchToolbar from './SearchToolbar';

export default observer(function SearchPanelPortal({
  editorStore: { searchPanel: searchPanelStore },
}: {
  editorStore: EditorStore;
}): JSX.Element | null {
  const { element: searchPanelContainer } = searchPanelStore;

  if (searchPanelContainer === undefined) {
    return null;
  }
  return (
    <Portal container={searchPanelContainer}>
      <SearchToolbar searchPanelStore={searchPanelStore} />
    </Portal>
  );
});
