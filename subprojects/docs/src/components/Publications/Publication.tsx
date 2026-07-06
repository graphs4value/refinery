import Link from '@docusaurus/Link';
import React from 'react';
import { useEffect, useId, useRef, useState } from 'react';

import styles from './Publication.module.css';

const specialAbbreviations: Record<string, string> = {
  'Csanád': 'Cs.',
};

export default function Publication({
  authors,
  title,
  venue,
  year,
  doi,
  links,
  abstract,
  bibtex,
  children,
  newLineBeforeLinks = true,
}: {
  authors?: { given: string; family: string }[];
  title?: string;
  venue?: string;
  year?: number;
  doi?: string;
  links?: { label: string; url: string }[];
  abstract?: string;
  bibtex?: string;
  children?: React.ReactNode;
  newLineBeforeLinks?: boolean;
}) {
  const [openSections, setOpenSections] = useState({
    bibtex: false,
    abstract: false,
  });
  const [copiedSection, setCopiedSection] = useState<'bibtex' | 'abstract' | null>(null);
  const bibtexId = useId();
  const abstractId = useId();
  const copiedTimeoutRef = useRef<number | null>(null);
  const doiLink = doi ? `https://doi.org/${doi}` : undefined;

  useEffect(() => {
    return () => {
      if (copiedTimeoutRef.current !== null) {
        window.clearTimeout(copiedTimeoutRef.current);
      }
    };
  }, []);

  function toggleSection(section: 'bibtex' | 'abstract') {
    setOpenSections((current) => ({
      ...current,
      [section]: !current[section],
    }));
  }

  async function copySection(section: 'bibtex' | 'abstract', text: string) {
    await navigator.clipboard.writeText(text);
    setCopiedSection(section);

    if (copiedTimeoutRef.current !== null) {
      window.clearTimeout(copiedTimeoutRef.current);
    }

    copiedTimeoutRef.current = window.setTimeout(() => {
      setCopiedSection((current) => (current === section ? null : current));
      copiedTimeoutRef.current = null;
    }, 1500);
  }

  return (
    <li className={styles['entry']}>
      <div className={styles['summary']}>
        {children}
        {authors && (
          <>
            {authors.map(abbreviateAuthor).join(', ')}:
          </>
        )}
        {title && (
          <>
            {' '}
            <em>{title}.</em>
          </>
        )}
        {venue && (
          <>
            {' '}
            {venue}
          </>
        )}
        {year && (
          <>
            {' '}
            ({year})
          </>
        )}
        {newLineBeforeLinks && (doi || links || abstract || bibtex) && (
          <>
            <br />
          </>
        )}
        {doi && (
          <>
            {' '}
            [<Link href={doiLink}>doi</Link>]
          </>
        )}
        {links && links.map((link) => {
          if (link.url !== doiLink) {
            return (
              <React.Fragment key={link.label}>
                {' '}
                [<Link href={prepareLink(link.url)}>{link.label}</Link>]
              </React.Fragment>
            );
          }
          return null;
        })}
        {abstract && (
          <>
            {' '}
            [
            <Link
              href="#"
              aria-expanded={openSections.abstract}
              aria-controls={abstractId}
              onClick={(event) => {
                event.preventDefault();
                toggleSection('abstract');
              }}
            >
              abstract {openSections.abstract ? '▴' : '▾'}
            </Link>
            ]
          </>
        )}
        {bibtex && (
          <>
            {' '}
            [
            <Link
              href="#"
              aria-expanded={openSections.bibtex}
              aria-controls={bibtexId}
              onClick={(event) => {
                event.preventDefault();
                toggleSection('bibtex');
              }}
            >
              bibtex {openSections.bibtex ? '▴' : '▾'}
            </Link>
            ]
          </>
        )}
      </div>
      {abstract && (
        <div
          className={`${styles['panel']} ${
            openSections.abstract ? styles['panel--open'] : ''
          }`}
          id={abstractId}
          aria-hidden={!openSections.abstract}
        >
          <button
            type="button"
            className={styles['copyButton']}
            onClick={() => copySection('abstract', abstract)}
          >
            {copiedSection === 'abstract' ? 'Copied' : 'Copy'}
          </button>
          <div className={styles['panel-inner']}>
            <pre className={styles['content']}>{abstract}</pre>
          </div>
        </div>
      )}
      {bibtex && (
        <div
          className={`${styles['panel']} ${
            openSections.bibtex ? styles['panel--open'] : ''
          }`}
          id={bibtexId}
          aria-hidden={!openSections.bibtex}
        >
          <button
            type="button"
            className={styles['copyButton']}
            onClick={() => copySection('bibtex', bibtex)}
          >
            {copiedSection === 'bibtex' ? 'Copied' : 'Copy'}
          </button>
          <div className={styles['panel-inner']}>
            <pre className={`${styles['content']} ${styles['mono']}`}>{bibtex}</pre>
          </div>
        </div>
      )}
    </li>
  );
}

function prepareLink(url: string): string {
  if (url.charAt(0) === '/') {
    return `pathname://${url}`;
  }
  return url;
}

function abbreviateAuthor(author: { given: string; family: string }): string {
  if (specialAbbreviations[author.given]) {
    return `${specialAbbreviations[author.given]} ${author.family}`;
  }
  return `${author.given.charAt(0)}. ${author.family}`;
}
