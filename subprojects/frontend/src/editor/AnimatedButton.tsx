import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import { styled } from '@mui/material/styles';
import { type ReactNode, useLayoutEffect, useState } from 'react';

const AnimatedButtonBase = styled(Button, {
  shouldForwardProp: (prop) => prop !== 'width',
})<{ width: string }>(({ theme, width }) => {
  // Transition copied from `@mui/material/Button`.
  const colorTransition = theme.transitions.create(
    ['background-color', 'box-shadow', 'border-color', 'color'],
    { duration: theme.transitions.duration.short },
  );
  return {
    width,
    // Make sure the button does not change width if a number is updated.
    fontVariantNumeric: 'tabular-nums',
    transition: `
      ${colorTransition},
      ${theme.transitions.create(['width'], {
        duration: theme.transitions.duration.short,
        easing: theme.transitions.easing.easeOut,
      })}
    `,
    '@media (prefers-reduced-motion: reduce)': {
      transition: colorTransition,
    },
  };
});

export default function AnimatedButton({
  'aria-label': ariaLabel,
  onClick,
  color,
  disabled,
  startIcon,
  children,
}: {
  'aria-label'?: string;
  onClick?: () => void;
  color: 'error' | 'warning' | 'primary' | 'inherit';
  disabled?: boolean;
  startIcon: JSX.Element;
  children?: ReactNode;
}): JSX.Element {
  const [width, setWidth] = useState<string | undefined>();
  const [contentsElement, setContentsElement] = useState<HTMLDivElement | null>(
    null,
  );

  useLayoutEffect(() => {
    if (contentsElement !== null) {
      const updateWidth = () => {
        setWidth(window.getComputedStyle(contentsElement).width);
      };
      updateWidth();
      const observer = new ResizeObserver(updateWidth);
      observer.observe(contentsElement);
      return () => observer.unobserve(contentsElement);
    }
    return () => {};
  }, [setWidth, contentsElement]);

  return (
    <AnimatedButtonBase
      {...(ariaLabel === undefined ? {} : { 'aria-label': ariaLabel })}
      {...(onClick === undefined ? {} : { onClick })}
      color={color}
      variant="outlined"
      className="rounded"
      disabled={disabled ?? false}
      startIcon={startIcon}
      width={width === undefined ? 'auto' : `calc(${width} + 50px)`}
    >
      <Box
        display="flex"
        flexDirection="row"
        justifyContent="end"
        overflow="hidden"
        width="100%"
      >
        <Box whiteSpace="nowrap" ref={setContentsElement}>
          {children}
        </Box>
      </Box>
    </AnimatedButtonBase>
  );
}

AnimatedButton.defaultProps = {
  'aria-label': undefined,
  onClick: undefined,
  disabled: false,
  children: undefined,
};
