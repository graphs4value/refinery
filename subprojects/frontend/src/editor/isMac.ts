/*
 * Copyright (C) 2018-2021 by Marijn Haverbeke <marijn@haverbeke.berlin> and others
 * Copyright (C) 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 *
 * The contents of this file were extracted from
 * https://github.com/codemirror/view/blob/32aa0e88e9053bc867731c5057c30565b251ea26/src/browser.ts
 */

const nav =
  typeof navigator !== 'undefined'
    ? navigator
    : { userAgent: '', vendor: '', platform: '', maxTouchPoints: 0 };
const ie_edge = /Edge\/(\d+)/.exec(nav.userAgent);
const ie_upto10 = /MSIE \d/.test(nav.userAgent);
const ie_11up = /Trident\/(?:[7-9]|\d{2,})\..*rv:(\d+)/.exec(nav.userAgent);
const ie = !!(ie_upto10 ?? ie_11up ?? ie_edge);
const safari = !ie && nav.vendor.includes('Apple Computer');
const ios =
  safari && (/Mobile\/\w+/.test(nav.userAgent) || nav.maxTouchPoints > 2);
const isMac: boolean = ios || nav.platform.includes('Mac');

export default isMac;
