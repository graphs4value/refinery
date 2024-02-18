<!--
  SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>

  SPDX-License-Identifier: EPL-2.0
-->

# Refinery

[![Build](https://github.com/graphs4value/refinery/actions/workflows/build.yml/badge.svg)](https://github.com/graphs4value/refinery/actions/workflows/build.yml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=graphs4value_refinery&metric=alert_status)](https://sonarcloud.io/dashboard?id=graphs4value_refinery) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=graphs4value_refinery&metric=coverage)](https://sonarcloud.io/dashboard?id=graphs4value_refinery)

Refinery provides consistent graph model generation by partial model _refinement_.

See the [Refinery tutorial](https://github.com/graphs4value/refinery-tutorials/tree/main/filesystem) for more information.

## [Graph-Solver-as-a-Service](https://refinery.services)

Visit [https://refinery.services](https://refinery.services) to try our Graph-Solver-as-a-Service supported by the [2022 Amazon Research Award](https://www.amazon.science/research-awards/recipients/daniel-varro-fall-2021).

## Running locally

To generate larger models with a longer timeout, you can use our [Docker container](https://github.com/graphs4value/refinery/pkgs/container/refinery) on either `amd64` or `arm64` machines:

```sh
docker run --rm -it -p 8888:8888 ghcr.io/graphs4value/refinery
```

Once Docker pulls and starts the container, you can navigate to [http://localhost:8888](http://localhost:8888) to open the model generation interface and start editing.

Alternatively, you can follow the instructions in [CONTRIBUTING.md](CONTRIBUTING.md) to set up a local development environment and compile and run Refinery from source.

## Related publications

### Tool demonstration

* K. Marussy, A. Ficsor, O. Semeráth, D. Varró: &ldquo;Refinery: Graph Solver as a Service&rdquo; _ICSE 2024 Demonstrations_ [[doi](https://doi.org/10.1145/3639478.3640045)] [[pdf](https://refinery.tools/papers/icse24-demo.pdf)] [[video](https://youtu.be/Qy_3udNsWsM)]

### Partial model specification language

* K. Marussy, O. Semeráth, A. Babikian, D. Varró: _A Specification Language for Consistent Model Generation based on Partial Models._
J. Object Technol. **19**(3): 3:1-22 (2020) [[doi](https://doi.org/10.5381/jot.2020.19.3.a12)] [[pdf](https://www.jot.fm/issues/issue_2020_03/article12.pdf)] [[video](https://www.youtube.com/watch?v=ggTbv_s5t2A)]

### Consitent graph generation techniques

* O. Semeráth, A. Nagy, D. Varró: &ldquo;A graph solver for the automated generation of consistent domain-specific models.&rdquo; _ICSE 2018:_ 969-980 [[doi](https://doi.org/10.1145/3180155.3180186)] [[pdf](https://dl.acm.org/doi/pdf/10.1145/3180155.3180186)]
* K. Marussy, O. Semeráth, D. Varró: _Automated Generation of Consistent Graph Models With Multiplicity Reasoning._ IEEE Trans. Software Eng. **48**(5): 1610-1629 (2022) [[doi](https://doi.org/10.1109/TSE.2020.3025732)] [[pdf](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9201551)]
* A.. Babikian, O. Semeráth, A. Li, K. Marussy, D. Varró: _Automated generation of consistent models using qualitative abstractions and exploration strategies._ Softw. Syst. Model. **21**(5): 1763-1787 (2022) [[doi](https://doi.org/10.1007/s10270-021-00918-6)] [[pdf](https://link.springer.com/content/pdf/10.1007/s10270-021-00918-6.pdf?pdf=button)]

### Diverse and realistic graph generation

* O. Semeráth, R. Farkas, G. Bergmann, D. Varró: _Diversity of graph models and graph generators in mutation testing._ Int. J. Softw. Tools Technol. Transf. **22**(1): 57-78 (2020) [[doi](https://doi.org/10.1007/s10009-019-00530-6)] [[pdf](https://link.springer.com/content/pdf/10.1007/s10009-019-00530-6.pdf?pdf=button)]
* O. Semeráth, A. Babikian, B. Chen, C. Li, K. Marussy, G. Szárnyas, D. Varró: _Automated generation of consistent, diverse and structurally realistic graph models._ Softw. Syst. Model. **20**(5): 1713-1734 (2021) [[doi](https://doi.org/10.1007/s10270-021-00884-z)] [[pdf](https://link.springer.com/content/pdf/10.1007/s10270-021-00884-z.pdf?pdf=button)]

### Correctness proofs

* D. Varró, O. Semeráth, G. Szárnyas, Á. Horváth: &ldquo;Towards the Automated Generation of Consistent, Diverse, Scalable and Realistic Graph Models.&rdquo; _Graph Transformation, Specifications, and Nets 2018:_ 285-312 [[doi](https://doi.org/10.1007/978-3-319-75396-6_16)] [[pdf](https://inf.mit.bme.hu/sites/default/files/publications/fmhe-model-generation.pdf)]

## License

Copyright (c) 2021-2024 [The Refinery Authors](CONTRIBUTORS.md)

Refinery is available under the [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/).

Refinery complies with the [REUSE Specification – Version 3.0](https://reuse.software/) to provide copyright and licensing information to each file, including files available under other licenses.
For more information, see the comments headers in each file and the license texts in the [LICENSES](LICENSES/) directory.
