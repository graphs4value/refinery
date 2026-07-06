/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import clsx from 'clsx';

import styles from './index.module.css';

import Features from '@site/src/components/Features';
import UseCases from '@site/src/components/UseCases';
import Video from '@site/src/components/Video';
import Publication from '@site/src/components/Publications/Publication';

function Hero() {
  return (
    <header className={clsx('hero', 'hero--dark', styles['hero'])}>
      <div className="container">
        <h1 className="hero__title">Refinery</h1>
        <p className="hero__subtitle">
          An efficient graph solver for generating well-formed models
        </p>
        <div className={styles['buttons']}>
          <Link
            href="https://refinery.services/"
            className={clsx(
              'button',
              'button--lg',
              'button--primary',
              styles['button'],
            )}
          >
            Try online
          </Link>
          <Link
            to="/learn/docker"
            className={clsx(
              'button',
              'button--lg',
              'button--secondary',
              styles['button'],
            )}
          >
            Try in Docker
          </Link>
          <Link
            to="/learn/tutorials/file-system"
            className={clsx(
              'button',
              'button--lg',
              'button--secondary',
              styles['button'],
            )}
          >
            Tutorial
          </Link>
        </div>
      </div>
    </header>
  );
}

function Publications() {
  return (
    <div>
      <div className="row">
        <div className="col col--6">
          <h3>Tool demonstration</h3>
          <ul>
            <Publication
              authors={[
                { given: 'Kristóf', family: 'Marussy' },
                { given: 'Attila', family: 'Ficsor' },
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'Dániel', family: 'Varró' },
              ]}
              title="Refinery: Graph Solver as a Service"
              venue="ICSE 2024 Demonstrations"
              year={2024}
              doi="10.1145/3639478.3640045"
              links={[
                { label: 'pdf', url: 'pathname:///papers/icse24-demo.pdf' },
                { label: 'video', url: 'https://youtu.be/Qy_3udNsWsM' },
              ]}
              newLineBeforeLinks={false}
            />
          </ul>
          <h3>Partial model specification language</h3>
          <ul>
            <Publication
              authors={[
                { given: 'Kristóf', family: 'Marussy' },
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'Aren A.', family: 'Babikian' },
                { given: 'Dániel', family: 'Varró' },
              ]}
              title="A Specification Language for Consistent Model Generation based on Partial Models"
              venue="J. Object Technol. 19 (3): 3:1-22"
              year={2020}
              doi="10.5381/jot.2020.19.3.a12"
              pdf="https://www.jot.fm/issues/issue_2020_03/article12.pdf"
              video="https://www.youtube.com/watch?v=ggTbv_s5t2A"
              newLineBeforeLinks={false}
            />
          </ul>
          <h3>Diverse and realistic graph generation</h3>
          <ul>
            <Publication
              authors={[
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'Rebeka', family: 'Farkas' },
                { given: 'Gábor', family: 'Bergmann' },
                { given: 'Dániel', family: 'Varró' },
              ]}
              title="Diversity of graph models and graph generators in mutation testing"
              venue="Int. J. Softw. Tools Technol. Transf. 22 (1): 57-78"
              year={2020}
              doi="10.1007/s10009-019-00530-6"
              pdf="https://link.springer.com/content/pdf/10.1007/s10009-019-00530-6.pdf?pdf=button"
              newLineBeforeLinks={false}
            />
            <Publication
              authors={[
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'Aren A.', family: 'Babikian' },
                { given: 'Boqi', family: 'Chen' },
                { given: 'Chuning', family: 'Li' },
                { given: 'Kristóf', family: 'Marussy' },
                { given: 'Gábor', family: 'Szárnyas' },
                { given: 'Dániel', family: 'Varró' },
              ]}
              title="Automated generation of consistent, diverse and structurally realistic graph models"
              venue="Softw. Syst. Model. 20 (5): 1713-1734"
              year={2021}
              doi="10.1007/s10270-021-00918-6"
              pdf="https://link.springer.com/content/pdf/10.1007/s10270-021-00884-z.pdf"
              newLineBeforeLinks={false}
            />
          </ul>
        </div>
        <div className="col col--6">
          <h3>Consistent graph generation techniques</h3>
          <ul>
            <Publication
              authors={[
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'András Szabolcs', family: 'Nagy' },
                { given: 'Dániel', family: 'Varró' },
              ]}
              title="A graph solver for the automated generation of consistent domain-specific models"
              venue="ICSE 2018: 969-980"
              year={2018}
              doi="10.1145/3180155.3180186"
              pdf="https://dl.acm.org/doi/pdf/10.1145/3180155.3180186"
              newLineBeforeLinks={false}
            />
            <Publication
              authors={[
                { given: 'Kristóf', family: 'Marussy' },
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'Dániel', family: 'Varró' },
              ]}
              title="Automated Generation of Consistent Graph Models With Multiplicity Reasoning"
              venue="IEEE Trans. Softw. Eng. 48 (5): 1610-1629"
              year={2022}
              doi="10.1109/TSE.2020.3025732"
              pdf="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9201551"
              newLineBeforeLinks={false}
            />
            <Publication
              authors={[
                { given: 'Aren A.', family: 'Babikian' },
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'Anqi', family: 'Li' },
                { given: 'Kristóf', family: 'Marussy' },
                { given: 'Dániel', family: 'Varró' },
              ]}
              title="Automated generation of consistent models using qualitative abstractions and exploration strategies"
              venue="Softw. Syst. Model. 21 (5): 1763-1787"
              year={2022}
              doi="10.1007/s10270-021-00918-6"
              pdf="https://link.springer.com/content/pdf/10.1007/s10270-021-00918-6.pdf?pdf=button"
              newLineBeforeLinks={false}
            />
          </ul>
          <h3>Correctness proofs</h3>
          <ul>
            <Publication
              authors={[
                { given: 'Dániel', family: 'Varró' },
                { given: 'Oszkár', family: 'Semeráth' },
                { given: 'Gábor', family: 'Szárnyas' },
                { given: 'Ákos', family: 'Horváth' },
              ]}
              title="Towards the Automated Generation of Consistent, Diverse, Scalable and Realistic Graph Models"
              venue="Graph Transformation, Specifications, and Nets 2018: 285-312"
              year={2018}
              doi="10.1007/978-3-319-75396-6_16"
              pdf="https://inf.mit.bme.hu/sites/default/files/publications/fmhe-model-generation.pdf"
              newLineBeforeLinks={false}
            />
          </ul>
        </div>
      </div>
      <div className={styles['section__actions']}>
        <Link
          to="/publications/"
          className={clsx(
            'button',
            'button--lg',
            'button--secondary',
            styles['button'],
          )}
        >
          All publications
        </Link>
      </div>
    </div>
  );
}

export default function Home() {
  return (
    <Layout>
      <div className={styles['page']}>
        <Hero />
        <section className={styles['section']}>
          <Features />
        </section>
        <section className={clsx(styles['section'], styles['section--video'])}>
          <Video />
        </section>
        <section className={styles['section']}>
          <div className="container">
            <h2 className={styles['section__title']}>Explore use-cases</h2>
            <UseCases />
          </div>
        </section>
        <section className={styles['section']}>
          <div className="container">
            <h2 className={styles['section__title']}>Related publications</h2>
            <Publications />
          </div>
        </section>
      </div>
    </Layout>
  );
}
