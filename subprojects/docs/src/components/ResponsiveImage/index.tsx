/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';

import ImageTag, { type Props as ImageTagProps } from './ImageTag';
import styles from './index.module.css';

interface Props extends ImageTagProps {
  original: string;
}

export default function ThemedImage({ original, ...props }: Props) {
  const {
    image: { width, height, placeholder },
  } = props;
  return (
    <div
      style={{ aspectRatio: `${width}/${height}` }}
      className={styles['container']}
    >
      <div
        style={{ backgroundImage: `url(${placeholder})` }}
        className={styles['placeholder']}
      />
      <Link href={`pathname://${original}`} className={String(styles['link'])}>
        <ImageTag {...props} />
      </Link>
    </div>
  );
}
