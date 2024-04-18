/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { useState } from 'react';

import coverBackground from './cover-background.png?sizes[]=1920&sizes[]=1288&sizes[]=1108&&sizes[]=644&sizes[]=322&placeholder=true&rl';
import Cover from './cover.svg';
import styles from './index.module.css';

export default function Video() {
  const [started, setStarted] = useState(false);
  return (
    <>
      <h2 className="sr-only">Check out the intro video</h2>
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
              aria-label="Video introduction"
              title="Play video (requires acceping cookies from YouTube)"
              onClick={() => setStarted(true)}
              className={styles['video__button']}
              style={{
                backgroundImage: `url("${coverBackground.placeholder}")`,
              }}
            >
              <img
                alt=""
                src={coverBackground.src}
                srcSet={coverBackground.srcSet}
                width={coverBackground.width}
                height={coverBackground.height}
                sizes="(min-width: 1440px) 1288px, (min-width: 1140px) 1108px, calc(100vw - 32px)"
                loading="lazy"
                className={styles['video__image']}
              />
              <div className={styles['video__svg']}>
                <Cover />
              </div>
            </button>
          )}
        </div>
      </div>
    </>
  );
}
