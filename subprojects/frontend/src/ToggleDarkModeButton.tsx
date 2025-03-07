/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import { observer } from 'mobx-react-lite';
import { useRef } from 'react';
import { flushSync } from 'react-dom';

import { useRootStore } from './RootStoreProvider';
import ThemeStore from './theme/ThemeStore';
import getLogger from './utils/getLogger';

const logger = getLogger('ToggleDarkModeButton');

function toggleWithViewTransition(
  themeStore: ThemeStore,
  button: HTMLElement | null,
): void {
  document.body.classList.add('notransition');
  let x = '100%';
  let y = '0%';
  let maxRadius = '100%';
  if (button !== null) {
    const {
      x: buttonX,
      y: buttonY,
      width,
      height,
    } = button.getBoundingClientRect();
    const { width: documentWidth, height: documentHeight } =
      document.documentElement.getBoundingClientRect();
    const centerX = buttonX + width / 2;
    const centerY = buttonY + height / 2;
    const offsetX = Math.max(centerX, documentWidth - centerX);
    const offsetY = Math.max(centerY, documentHeight - centerY);
    x = `${centerX}px`;
    y = `${centerY}px`;
    maxRadius = `${Math.sqrt(offsetX ** 2 + offsetY ** 2)}px`;
  }
  document.documentElement.style.setProperty('--origin-x', x);
  document.documentElement.style.setProperty('--origin-y', y);
  document.documentElement.style.setProperty('--max-radius', maxRadius);
  const transition = document.startViewTransition(() => {
    flushSync(() => themeStore.toggleDarkMode());
  });
  transition.finished
    .finally(() => {
      document.body.classList.remove('notransition');
    })
    .catch((err: unknown) => {
      logger.error({ err }, 'Transition failed when toggling dark mode');
    });
}

function toggleWithoutViewTransition(themeStore: ThemeStore): void {
  document.body.classList.add('notransition');
  try {
    flushSync(() => themeStore.toggleDarkMode());
  } finally {
    document.body.classList.remove('notransition');
  }
}

export default observer(function ToggleDarkModeButton(): React.ReactElement {
  const { themeStore } = useRootStore();
  const { darkMode } = themeStore;
  const buttonRef = useRef<HTMLButtonElement | null>(null);

  const callback = () => {
    if (
      'startViewTransition' in document ||
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      toggleWithViewTransition(themeStore, buttonRef.current);
    } else {
      toggleWithoutViewTransition(themeStore);
    }
  };

  return (
    <Tooltip title={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}>
      <IconButton color="inherit" onClick={callback} ref={buttonRef}>
        {darkMode ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
      </IconButton>
    </Tooltip>
  );
});
