/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { useId, type FunctionComponent, type SVGAttributes } from 'react';

import styles from './index.module.css';

export default function SVG({
  Component,
  alt,
  title,
}: {
  Component: FunctionComponent<SVGAttributes<SVGElement> & { title?: string }>;
  alt?: string;
  title?: string;
}) {
  const labelID = useId();

  return (
    <div className={styles['container']}>
      <Component
        role="img"
        {...(title === undefined ? {} : { title })}
        {...(alt === undefined ? {} : { 'aria-labelledby': labelID })}
      />
      {alt !== undefined && (
        <div id={labelID} className="sr-only">
          {alt}
        </div>
      )}
    </div>
  );
}
