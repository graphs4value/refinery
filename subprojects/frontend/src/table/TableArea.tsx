/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { observer } from 'mobx-react-lite';

import Loading from '../Loading';
import { useRootStore } from '../RootStoreProvider';

import RelationGrid from './RelationGrid';

function TablePane(): JSX.Element {
  const { editorStore } = useRootStore();

  if (editorStore === undefined) {
    return <Loading />;
  }

  return <RelationGrid graph={editorStore.graph} />;
}

export default observer(TablePane);
