/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';

import styles from './index.module.css';

export default function Home() {
  return (
    <Layout>
      <header className="hero hero--dark">
        <div className="container">
          <h1 className="hero__title">Refinery</h1>
          <p className="hero__subtitle">
            An efficient graph solver for generating well-formed models
          </p>
          <div className={styles['buttons']}>
            <Link
              href="https://refinery.services/"
              className="button button--lg button--primary"
            >
              Try online
            </Link>
            <Link
              to="/docs/docker"
              className="button button--lg button--secondary"
            >
              Try in Docker
            </Link>
            <Link
              to="/docs/tutorials/file-system"
              className="button button--lg button--secondary"
            >
              Tutorial
            </Link>
          </div>
        </div>
      </header>
    </Layout>
  );
}
