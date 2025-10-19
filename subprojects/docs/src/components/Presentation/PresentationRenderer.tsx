/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';
import clsx from 'clsx';
import { useId, useEffect, useState } from 'react';
import { useInView } from 'react-intersection-observer';
import { pdfjs, Document, Page } from 'react-pdf';
import 'react-pdf/dist/Page/AnnotationLayer.css';
import 'react-pdf/dist/Page/TextLayer.css';
import { useResizeDetector } from 'react-resize-detector';

import styles from './index.module.css';

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url,
).toString();

function Message({ message }: { message: string }) {
  return <div className={styles['message']}>{message}</div>;
}

const settingName = 'pipEnabled';

function useSetting(): [boolean, (newValue: boolean) => void] {
  const [value, setValue] = useState(
    window.localStorage?.getItem(settingName) !== 'false',
  );
  useEffect(() => {
    const listener = (event: StorageEvent) => {
      if (event.key === settingName) {
        setValue(event.newValue !== 'false');
      }
    };
    window.addEventListener('storage', listener);
    return () => {
      window.removeEventListener('storage', listener);
    };
  }, []);
  return [
    value,
    (newValue: boolean) => {
      window.localStorage?.setItem(settingName, String(newValue));
      setValue(newValue);
    },
  ];
}

export default function PresentationRenderer({ src }: { src: string }) {
  const [pipEnabled, setPipEnabled] = useSetting();
  const [loaded, setLoaded] = useState(false);
  const [pages, setPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const { inView, ref: intersectionRef } = useInView({
    threshold: 0.5,
    initialInView: true,
    fallbackInView: true,
  });
  const { width, ref: resizeRef } = useResizeDetector({
    refreshMode: 'throttle',
    refreshRate: 200,
  });
  const pipCheckboxId = useId();

  const pip = loaded && pipEnabled && !inView;
  const buttonSize = `${(width ?? 200) * 0.15}px`;
  const pageNumberWidth = `${pages > 0 ? Math.ceil(Math.log10(pages)) : 1}ch`;
  const linkClicked = (event: MouseEvent) => {
    const { target } = event;
    if (target !== null && 'tagName' in target && target.tagName === 'A') {
      const href = (target as HTMLAnchorElement).href;
      let url;
      try {
        url = new URL(href);
      } catch {
        // Malformed URL, ignore.
        return;
      }
      if (url.host === 'scroll-to-section') {
        event.preventDefault();
        window.location.replace(url.hash);
      }
    }
  };

  return (
    <>
      <div ref={intersectionRef}>
        <div ref={resizeRef} className={styles['placeholder']}>
          <div
            style={width === undefined ? {} : { width }}
            className={clsx(
              styles['wrapper'],
              pip && styles['wrapper--pip'],
              pip && 'shadow--tl',
            )}
          >
            <Document
              file={src}
              loading={<Message message="Loading presentation" />}
              error={<Message message="Failed to load presentation" />}
              onLoadSuccess={(pdf) => {
                setLoaded(true);
                setPages(pdf.numPages);
              }}
              onItemClick={({ pageNumber }) => {
                setCurrentPage(pageNumber);
              }}
              externalLinkTarget="_blank"
            >
              <Page
                pageNumber={currentPage}
                width={width!}
                onClick={linkClicked}
              />
            </Document>
            {loaded && currentPage > 1 && (
              <button
                className={clsx(styles['nav'], styles['nav--previous'])}
                style={{ fontSize: buttonSize }}
                onClick={(event) =>
                  setCurrentPage(event.shiftKey ? 1 : currentPage - 1)
                }
                aria-label="Previous slide"
              >
                &lsaquo;
              </button>
            )}
            {loaded && currentPage < pages && (
              <button
                className={clsx(styles['nav'], styles['nav--next'])}
                style={{ fontSize: buttonSize }}
                onClick={(event) =>
                  setCurrentPage(event.shiftKey ? pages : currentPage + 1)
                }
                aria-label="Previous slide"
              >
                &rsaquo;
              </button>
            )}
          </div>
        </div>
      </div>
      <div className={styles['navbar']}>
        <button
          className={clsx(
            'button',
            'button--sm',
            'button--secondary',
            styles['button--first'],
          )}
          disabled={!loaded || currentPage <= 1}
          onClick={() => setCurrentPage(1)}
          aria-label="First slide"
        >
          First
        </button>
        <button
          className={clsx(
            'button',
            'button--sm',
            'button--secondary',
            styles['button--prev'],
          )}
          disabled={!loaded || currentPage <= 1}
          onClick={() => setCurrentPage(currentPage - 1)}
          aria-label="Previous slide"
        >
          Prev
        </button>
        {loaded && (
          <div>
            <span
              className={styles['page-number']}
              style={{ width: pageNumberWidth }}
            >
              {currentPage}
            </span>{' '}
            of {pages}
          </div>
        )}
        <button
          className={clsx(
            'button',
            'button--sm',
            'button--secondary',
            styles['button--next'],
          )}
          disabled={!loaded || currentPage >= pages}
          onClick={() => setCurrentPage(currentPage + 1)}
          aria-label="Next slide"
        >
          Next
        </button>
        <button
          className={clsx(
            'button',
            'button--sm',
            'button--secondary',
            styles['button--last'],
          )}
          disabled={!loaded || currentPage >= pages}
          onClick={() => setCurrentPage(pages)}
          aria-label="Last slide"
        >
          Last
        </button>
        <Link
          to={`pathname://${src}`}
          download
          className="button button--sm button--primary"
        >
          Download
        </Link>
        <div>
          <input
            type="checkbox"
            id={pipCheckboxId}
            checked={pipEnabled}
            onChange={() => setPipEnabled(!pipEnabled)}
          />
          <label htmlFor={pipCheckboxId}>Scroll overlay</label>
        </div>
      </div>
    </>
  );
}
