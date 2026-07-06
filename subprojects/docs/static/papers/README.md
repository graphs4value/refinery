# Publication Administration

There are three *bib* files used for administering publications related to Refinery: one for proper publications, theses, and students' scientific conference (TDK) reports, respectively. To add a new publication, add a new entry to the relevant *bib* file.

You may add arbitrary links to the entry by starting the property name with `url_` (e.g., `url_pdf` will render a `pdf` label that is a link to the URL stored by the value associated to this property in the bib file).

Each entry *should* at least include the author(s), the title, the publication venue and year. Preferably, include a DOI, and a link to a PDF. You may also add the abstract.

Theses and TDK reports should include a note regarding the type of the thesis and the name of the institute, e.g.:

- `note = {PhD Thesis, Budapest University of Technology and Economics}`
- `note = {Master's Thesis, McGill University}`
- `note = {Students' Scientific Conference, Budapest University of Technology and Economics}`

The type of a thesis entry should be `Thesis` and the type of a TDK entry should be `Report`.

If you want a special abbreviation for a name (e.g., `Csanád` - `Cs.`), add it to the `specialAbbreviations` in [Publication.tsx](../../src/components/Publications/Publication.tsx#L7).
