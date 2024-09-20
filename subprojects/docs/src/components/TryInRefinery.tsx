/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Link from '@docusaurus/Link';

export default function TryInRefinery({ href }: { href: string }) {
  return (
    <p>
      <Link
        href={href}
        className="button button--primary button--outline button--play"
      >
        Try in Refinery
      </Link>
    </p>
  );
}
