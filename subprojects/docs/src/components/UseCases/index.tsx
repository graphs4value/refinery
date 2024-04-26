/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';
import clsx from 'clsx';

import styles from './index.module.css';
import Uc1 from './uc1.svg';
import Uc2 from './uc2.svg';
import Uc3 from './uc3.svg';
import Uc4 from './uc4.svg';
import Uc5 from './uc5.svg';
import Uc6 from './uc6.svg';

function UseCase({
  icon,
  title,
  href,
}: {
  icon: React.ReactNode;
  title: React.ReactNode;
  href: string;
}) {
  return (
    <div className="col col--4">
      <div className={styles['use-case']}>
        <h3 className={styles['use-case__title']}>
          <Link href={href} className={styles['use-case__link']!}>
            {title}
          </Link>
        </h3>
        <div className={styles['use-case__content']}>{icon}</div>
      </div>
    </div>
  );
}

export default function UseCases() {
  return (
    <>
      <div className="row">
        <UseCase
          icon={<Uc1 />}
          title={
            <>
              <b>Scenario generation</b> for testing autonomous vechicles
            </>
          }
          href="https://doi.org/10.1007/s10270-021-00884-z"
        />
        <UseCase
          icon={<Uc2 />}
          title={
            <>
              <b>Conformance checking</b> of modeling toolchains
            </>
          }
          href="https://doi.org/10.1007/s10009-019-00530-6"
        />
        <UseCase
          icon={<Uc3 />}
          title={
            <>
              Synthesize distributed <b>communication networks</b>
            </>
          }
          href="https://doi.org/10.1109/TSE.2020.3025732"
        />
      </div>
      <div className={clsx('row', styles['row--bottom'])}>
        <UseCase
          icon={<Uc4 />}
          title={
            <>
              <b>Execution time analysis</b> for <span>data-driven</span>{' '}
              critical systems
            </>
          }
          href="https://doi.org/10.1145/3471904"
        />
        <UseCase
          icon={<Uc5 />}
          title={
            <>
              <b>Generative architectures</b> with assured resilience
            </>
          }
          href="https://doi.org/10.1145/3550355.3552448"
        />
        <UseCase
          icon={<Uc6 />}
          title={
            <>
              <b>Video game map generator</b> with <span>model-based</span>{' '}
              techniques
            </>
          }
          href="https://doi.org/10.1145/3417990.3422001"
        />
      </div>
    </>
  );
}
