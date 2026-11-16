import { Cite } from '@citation-js/core';
import '@citation-js/plugin-bibtex';
import { useState, useEffect } from 'react';
import bibtexParse from 'bibtex-parse-js';
import latexToUnicode from 'latex-to-unicode'

import Publication from './Publication';

export default function BibPublications({ bib }: { bib: string }) {
  const [entries, setEntries] = useState<any[]>([]);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    async function loadBib() {
      try {
        const response = await fetch(bib);

        if (!response.ok) {
          throw new Error(
            `Failed to fetch BibTeX file "${bib}": ${response.statusText}`
          );
        }

        const bibText = await response.text();
        const cite = new Cite(bibText);
        const json = bibtexParse.toJSON(bibText);
        cite.data.forEach((element, index) => {
          element.raw = json[index];
        });
        console.log(cite);
        setEntries(cite.data);
      } catch (err) {
        setError(err instanceof Error ? err : new Error(String(err)));
      }
    }

    loadBib();
  }, [bib]);

  if (error) {
    return <div>{error.message}</div>;
  }

  return (
    <>
      <ul>
      {entries.sort((a, b) => (year(b) || 0) - (year(a) || 0)).map((entry, index) => (
        <div key={index} className="publication-entry">
          <Publication
            authors={entry.author?.map((author: any) => ({
              given: author.given,
              family: author.family,
            }))}
            title={entry.title}
            venue={entry["container-title"] || entry.raw.entryTags["note"]}
            year={year(entry)}
            doi={entry.DOI}
            links={[
              ...(entry.URL ? [{ label: 'link', url: entry.URL }] : []),
              ...Object.entries(entry.raw.entryTags)
                .filter(([key]) => key.startsWith('url_'))
                .map(([key, value]) => ({
                  label: key.replace(/^url_/, ''),
                  url: value,
                })),
            ].filter((value, index, self) => index === self.findIndex((t) => t.url === value.url))}
            link={entry.URL || entry.raw.entryTags["url"] || entry.raw.entryTags["url_link"]}
            pdf={entry.raw.entryTags["url_pdf"]}
            slides={entry.raw.entryTags["url_slides"]}
            video={entry.raw.entryTags["url_video"]}
            bibtex={entryToBibtex(entry)}
            abstract={entry.abstract}
          />
        </div>
      ))}
      </ul>
    </>
  );
}

function year(entry: any): number | null {
  return entry.issued?.["date-parts"]?.[0]?.[0];
}

function entryToBibtex(entry: any): string {
  const raw = entry.raw;
  const maxKeyLength = Math.max(...Object.keys(raw.entryTags).map((key) => key.length));
  return `@${raw.entryType}{${raw.citationKey},
${Object.entries(raw.entryTags)
  .map(([key, value]) => `  ${key.padEnd(maxKeyLength)} = {${value}}`)
  .join(',\n')}
}`;
}
