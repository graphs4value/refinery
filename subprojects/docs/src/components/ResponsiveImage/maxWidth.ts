/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export default function maxWidth(image: ResponsiveImageOutput): number {
  let result = 0;
  image.images.forEach(({ width }) => {
    if (width > result) {
      result = width;
    }
  });
  return result;
}
