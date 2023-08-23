/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import DotGraphVisualizer from './DotGraphVisualizer';
import ZoomCanvas from './ZoomCanvas';

export default function GraphArea(): JSX.Element {
  return (
    <ZoomCanvas>
      <DotGraphVisualizer />
    </ZoomCanvas>
  );
}
