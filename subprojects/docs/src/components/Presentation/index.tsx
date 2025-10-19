/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import BrowserOnly from '@docusaurus/BrowserOnly';
import Link from '@docusaurus/Link';
import { lazy, Suspense } from 'react';

import styles from './index.module.css';

const PresentationRenderer = lazy(() => import('./PresentationRenderer'));

function Fallback({ src }: { src: string }) {
  return (
    <>
      <div className={styles['placeholder']}>
        <div className={styles['wrapper']}>
          <div className={styles['message']}>
            Loading presentation
            <Link
              to={`pathname://${src}`}
              className="button button--outline button--secondary"
            >
              Open in new window
            </Link>
          </div>
        </div>
      </div>
      <div className={styles['navbar']}>
        <Link
          to={`pathname://${src}`}
          download
          className="button button--sm button--primary"
        >
          Download
        </Link>
      </div>
    </>
  );
}

export default function Presentation({ src }: { src: string }) {
  return (
    <>
      <BrowserOnly fallback={<Fallback src={src} />}>
        {() => (
          <Suspense fallback={<Fallback src={src} />}>
            <PresentationRenderer src={src} />
          </Suspense>
        )}
      </BrowserOnly>
    </>
  );
}
