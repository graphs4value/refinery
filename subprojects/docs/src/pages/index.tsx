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

function Publication({
  doi,
  pdf,
  video,
  children,
}: {
  doi?: string;
  pdf?: string;
  video?: string;
  children?: React.ReactNode;
}) {
  return (
    <li>
      {children}
      {doi && (
        <>
          {' '}
          [<Link href={`https://doi.org/${doi}`}>doi</Link>]
        </>
      )}
      {pdf && (
        <>
          {' '}
          [<Link href={pdf}>pdf</Link>]
        </>
      )}
      {video && (
        <>
          {' '}
          [<Link href={video}>video</Link>]
        </>
      )}
    </li>
  );
}

Publication.defaultProps = {
  doi: undefined,
  pdf: undefined,
  video: undefined,
  children: undefined,
};

function Publications() {
  return (
    <div className="row">
      <div className="col col--6">
        <h3>Tool demonstration</h3>
        <ul>
          <Publication
            doi="10.1145/3639478.3640045"
            pdf="pathname:///papers/icse24-demo.pdf"
            video="https://youtu.be/Qy_3udNsWsM"
          >
            K. Marussy, A. Ficsor, O. Semeráth, D. Varró: &ldquo;Refinery: Graph
            Solver as a Service&rdquo; <em>ICSE 2024 Demonstrations</em>
          </Publication>
        </ul>
        <h3>Partial model specification language</h3>
        <ul>
          <Publication
            doi="10.5381/jot.2020.19.3.a12"
            pdf="https://www.jot.fm/issues/issue_2020_03/article12.pdf"
            video="https://www.youtube.com/watch?v=ggTbv_s5t2A"
          >
            K. Marussy, O. Semeráth, A. Babikian, D. Varró:{' '}
            <em>
              A Specification Language for Consistent Model Generation based on
              Partial Models.
            </em>{' '}
            J. Object Technol. <b>19</b>(3): 3:1-22 (2020)
          </Publication>
        </ul>
        <h3>Diverse and realistic graph generation</h3>
        <ul>
          <Publication
            doi="10.1007/s10009-019-00530-6"
            pdf="https://link.springer.com/content/pdf/10.1007/s10009-019-00530-6.pdf?pdf=button"
          >
            O. Semeráth, R. Farkas, G. Bergmann, D. Varró:{' '}
            <em>
              Diversity of graph models and graph generators in mutation
              testing.
            </em>{' '}
            Int. J. Softw. Tools Technol. Transf. <b>22</b>(1): 57-78 (2020)
          </Publication>
          <Publication
            doi="10.1007/s10270-021-00884-z"
            pdf="https://link.springer.com/content/pdf/10.1007/s10270-021-00884-z.pdf?pdf=button"
          >
            O. Semeráth, A. Babikian, B. Chen, C. Li, K. Marussy, G. Szárnyas,
            D. Varró:{' '}
            <em>
              Automated generation of consistent, diverse and structurally
              realistic graph models.
            </em>{' '}
            Softw. Syst. Model. <b>20</b>(5): 1713-1734 (2021)
          </Publication>
        </ul>
      </div>
      <div className="col col--6">
        <h3>Consistent graph generation techniques</h3>
        <ul>
          <Publication
            doi="10.1145/3180155.3180186"
            pdf="https://dl.acm.org/doi/pdf/10.1145/3180155.3180186"
          >
            O. Semeráth, A. Nagy, D. Varró: &ldquo;A graph solver for the
            automated generation of consistent domain-specific models.&rdquo;{' '}
            <em>ICSE 2018:</em> 969-980
          </Publication>
          <Publication
            doi="10.1109/TSE.2020.3025732"
            pdf="https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9201551"
          >
            K. Marussy, O. Semeráth, D. Varró:{' '}
            <em>
              Automated Generation of Consistent Graph Models With Multiplicity
              Reasoning.
            </em>{' '}
            IEEE Trans. Softw. Eng. <b>48</b>(5): 1610-1629 (2022)
          </Publication>
          <Publication
            doi="10.1007/s10270-021-00918-6"
            pdf="https://link.springer.com/content/pdf/10.1007/s10270-021-00918-6.pdf?pdf=button"
          >
            A. Babikian, O. Semeráth, A. Li, K. Marussy, D. Varró:{' '}
            <em>
              Automated generation of consistent models using qualitative
              abstractions and exploration strategies.
            </em>{' '}
            Softw. Syst. Model. <b>21</b>(5): 1763-1787 (2022)
          </Publication>
        </ul>
        <h3>Correctness proofs</h3>
        <ul>
          <Publication
            doi="10.1007/978-3-319-75396-6_16"
            pdf="https://inf.mit.bme.hu/sites/default/files/publications/fmhe-model-generation.pdf"
          >
            D. Varró, O. Semeráth, G. Szárnyas, Á. Horváth: &ldquo;Towards the
            Automated Generation of Consistent, Diverse, Scalable and Realistic
            Graph Models.&rdquo;{' '}
            <em>Graph Transformation, Specifications, and Nets</em> 2018:
            285-312
          </Publication>
        </ul>
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
