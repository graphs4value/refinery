/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Stack from '@mui/material/Stack';
import { styled } from '@mui/material/styles';
import stringify from 'json-stringify-pretty-compact';
import { observer } from 'mobx-react-lite';

import { useRootStore } from '../RootStoreProvider';

const StyledCode = styled('code')(({ theme }) => ({
  ...theme.typography.editor,
  fontWeight: theme.typography.fontWeightEditorNormal,
  margin: theme.spacing(2),
  whiteSpace: 'pre',
}));

export default observer(function GraphPane(): JSX.Element {
  const { editorStore } = useRootStore();
  return (
    <Stack direction="column" height="100%" overflow="auto">
      <StyledCode>{stringify(editorStore?.semantics ?? {})}</StyledCode>
    </Stack>
  );
});
