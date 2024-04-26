/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import clsx from 'clsx';

import Fi1 from './fi1.svg';
import Fi2 from './fi2.svg';
import Fi3 from './fi3.svg';
import Fi4 from './fi4.svg';
import Fi5 from './fi5.svg';
import styles from './index.module.css';

function Feature({
  icon,
  title,
  offset,
  even,
  children,
}: {
  icon: React.ReactNode;
  title: string;
  offset?: number;
  even?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div
      className={clsx(
        'col',
        'col--4',
        { [`col--offset-${offset}`]: offset !== undefined },
        styles['feature__container'],
      )}
    >
      <div
        className={clsx(styles['feature'], {
          [styles['feature--even']!]: even,
        })}
      >
        <div className={styles['feature__icon']}>{icon}</div>
        <div className={styles['feature__contents']}>
          <h3 className={styles['feature__title']}>{title}</h3>
          <p className={styles['feature__text']}>{children}</p>
        </div>
      </div>
    </div>
  );
}

Feature.defaultProps = {
  offset: undefined,
  even: false,
};

export default function Features() {
  return (
    <div className="container">
      <svg xmlns="ttp://www.w3.org/2000/svg" className={styles['lg']}>
        <defs>
          <linearGradient
            id="fi-lg"
            x1="0"
            y1="0"
            x2="0"
            y2="366"
            gradientUnits="userSpaceOnUse"
          >
            <stop offset="0%" className={styles['lg__start']} />
            <stop offset="100%" className={styles['lg__end']} />
          </linearGradient>
        </defs>
      </svg>
      <h2 className="sr-only">Features</h2>
      <div className="row">
        <Feature icon={<Fi1 />} title="Diverse graph generation">
          Refinery provides a framework for the automated generation of graphs.
        </Feature>
        <Feature icon={<Fi2 />} title="Model with uncertainty" even>
          Partial modeling allows us to explicitly represent unknown or
          uncertain knowledge in our models. The Refinery framework enables us
          to explore design alternatives systematically.
        </Feature>
        <Feature icon={<Fi3 />} title="Formal logic reasoning">
          Refinery combines the mathematical precision of formal logic
          structures with the expressiveness of graph-based models. Underlying
          solver algorithms ensure formal correctness and completeness of
          generation processes.
        </Feature>
      </div>
      <div className={clsx('row', styles['row--last'])}>
        <Feature
          icon={<Fi4 />}
          title="Advanced web-based editor"
          offset={2}
          even
        >
          Designers are supported with state-of-the-art web-based editors with
          advanced IDE features and visualization techniques. The framework can
          be applied as a simple command-line interface program or deployed on
          the cloud.
        </Feature>
        <Feature icon={<Fi5 />} title="Powerful graph algorithms">
          Refinery is equipped with powerful algorithms such as incremental
          query evaluation, efficient graph isomorphism checking, and
          version-controlled data structures to solve various modeling and graph
          processing problems.
        </Feature>
      </div>
    </div>
  );
}
