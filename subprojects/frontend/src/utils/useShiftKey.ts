/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { useEffect, useState } from 'react';

export default function useShiftKey(): boolean {
  const [shiftDown, setShiftDown] = useState(false);

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      setShiftDown(event.shiftKey);
    };
    const handlePointer = (event: PointerEvent) => {
      setShiftDown(event.shiftKey);
    };
    const handleBlur = () => setShiftDown(false);
    window.addEventListener('keydown', handleKey, true);
    window.addEventListener('keyup', handleKey, true);
    window.addEventListener('pointerdown', handlePointer, true);
    window.addEventListener('pointermove', handlePointer, true);
    window.addEventListener('blur', handleBlur);
    return () => {
      window.removeEventListener('keydown', handleKey, true);
      window.removeEventListener('keyup', handleKey, true);
      window.removeEventListener('pointerdown', handlePointer, true);
      window.removeEventListener('pointermove', handlePointer, true);
      window.removeEventListener('blur', handleBlur);
    };
  }, []);

  return shiftDown;
}
