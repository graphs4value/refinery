/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';
import { useColorMode } from '@docusaurus/theme-common';
import useIsBrowser from '@docusaurus/useIsBrowser';
import clsx from 'clsx';

import ImageTag, { type Props as ImageTagProps } from './ImageTag';
import styles from './index.module.css';
import maxWidth from './maxWidth';

interface Props extends Omit<ImageTagProps, 'image'> {
  light: ResponsiveImageOutput;
  dark: ResponsiveImageOutput;
  originalLight: string;
  originalDark: string;
}

export default function Themed({
  light,
  dark,
  originalLight,
  originalDark,
  ...props
}: Props) {
  // Force re-render in browser.
  // https://github.com/facebook/docusaurus/blob/e012e0315862b2ca02cad40c58d11d31c319ff75/packages/docusaurus-theme-classic/src/theme/CodeBlock/index.tsx#L32-L36
  const isBrowser = useIsBrowser();
  const { colorMode } = useColorMode();
  const isDark = colorMode === 'dark';
  const image = isDark ? dark : light;
  const original = isDark ? originalDark : originalLight;
  const imageTag = <ImageTag image={image} {...props} />;
  const responsiveTag = isBrowser ? imageTag : <noscript>{imageTag}</noscript>;

  if (light.width !== dark.width || light.height !== dark.height) {
    throw new Error(
      `Image size mismatch: ${light.src} is ${light.width}×${light.height}, but ${dark.src} is ${dark.width}×${dark.height}`,
    );
  }

  return (
    <div
      style={{
        aspectRatio: `${light.width}/${light.height}`,
        maxWidth: maxWidth(light),
      }}
      className={styles['container']}
    >
      <div
        style={{ backgroundImage: `url(${light.placeholder})` }}
        className={styles['placeholder']}
      />
      <div
        style={{ backgroundImage: `url(${dark.placeholder})` }}
        className={clsx(styles['placeholder'], styles['placeholder--dark'])}
      />
      <Link href={`pathname://${original}`} className={String(styles['link'])}>
        {responsiveTag}
      </Link>
    </div>
  );
}
