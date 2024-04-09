/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';
// import PlayCircle from 'vg';
import Layout from '@theme/Layout';
import clsx from 'clsx';
import { useState } from 'react';

import styles from './index.module.css';
import videoCover from './video-cover.webp?url';

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
            to="/docs/docker"
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
            to="/docs/tutorials/file-system"
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

function Video() {
  const [started, setStarted] = useState(false);
  return (
    <section className={clsx(styles['section'], styles['section--video'])}>
      <div className="container">
        <div className={styles['video__container']}>
          {started ? (
            <iframe
              width="560"
              height="315"
              src="https://www.youtube-nocookie.com/embed/Qy_3udNsWsM?autoplay=1"
              title="YouTube video player"
              frameBorder="0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerPolicy="strict-origin-when-cross-origin"
              allowFullScreen
              className={styles['video']}
            />
          ) : (
            <button
              type="button"
              title="Play video (requires acceping cookies from YouTube)"
              onClick={() => setStarted(true)}
              className={styles['video__button']}
            >
              <h2>Modeling with Graphs</h2>
              <p>
                Graph based models are widely used in software engineering for
                systems models, the analysis of data structures, databases, and
                AI test environments.
              </p>
              <p>
                Testing, benchmarking or design-space exploration scnearios rely
                on the automated generation of consistent models!
              </p>
              <img src={videoCover} alt="" className={styles['video__cover']} />
              <div className={styles['video__play']} />
            </button>
          )}
        </div>
      </div>
    </section>
  );
}

export default function Home() {
  return (
    <Layout>
      <Hero />
      <Video />
    </Layout>
  );
}
