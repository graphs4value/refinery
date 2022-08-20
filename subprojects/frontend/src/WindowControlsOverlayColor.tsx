import { useTheme } from '@mui/material/styles';
import { useEffect } from 'react';

export default function WindowControlsOverlayColor(): null {
  const {
    palette: {
      outer: { background },
    },
  } = useTheme();
  useEffect(() => {
    document.head
      .querySelectorAll('meta[name="theme-color"]')
      .forEach((meta) => meta.remove());
    const meta = document.createElement('meta');
    meta.name = 'theme-color';
    meta.content = background;
    document.head.appendChild(meta);
  }, [background]);

  return null;
}
