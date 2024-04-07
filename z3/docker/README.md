<!--
  SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>

  SPDX-License-Identifier: Apache-2.0
-->

# Scripts for cross-building Z3 for linux-aarch64

We intentionally build in Ubuntu Focal to avoid a dependency on glibc 2.35.
See https://github.com/Z3Prover/z3/issues/6572 for details.

This should not be necessary once Z3 4.12.3 is released, as now Z3 nighly
includes linux-aarch64 builds.
See https://github.com/Z3Prover/z3/issues/6835 for details.
