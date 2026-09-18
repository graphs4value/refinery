/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { observer } from 'mobx-react-lite';

import SVGIcons from '../graph/SVGIcons';

import HeadlessGraphRenderer from './HeadlessGraphRenderer';
import type HeadlessStore from './HeadlessStore';

function HeadlessApp({ store }: { store: HeadlessStore }) {
  return (
    <>
      <SVGIcons />
      {...Array.from(store.pendingRequests.entries()).map(([id, request]) => (
        <HeadlessGraphRenderer key={id} request={request} />
      ))}
    </>
  );
}

export default observer(HeadlessApp);
