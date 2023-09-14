/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export default function svgURL(svg: string): string {
  return `url('data:image/svg+xml;utf8,${svg}')`;
}
