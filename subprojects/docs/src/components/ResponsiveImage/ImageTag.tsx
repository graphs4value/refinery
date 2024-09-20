/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import styles from './index.module.css';

export interface Props {
  image: ResponsiveImageOutput;
  alt?: string | undefined;
  title?: string | undefined;
}

export default function ImageTag({ image, alt, title }: Props) {
  return (
    <img
      alt={alt}
      title={title}
      width={image.width}
      height={image.height}
      sizes="(min-width: 1440px) 1320px, (max-width: 996px) calc(100vw - 32px), calc(75vw - 257px)"
      loading="lazy"
      src={image.src}
      srcSet={image.srcSet}
      className={styles['image']}
    />
  );
}
