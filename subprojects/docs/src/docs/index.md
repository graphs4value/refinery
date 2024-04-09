---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
sidebar_position: 0
---

# Introduction

Various software and systems engineering scenarios rely on the systematic construction of consistent graph models. However, **automatically generating a diverse set of consistent graph models** for complex domain specifications is challenging. First, the graph generation problem must be specified with mathematical precision. Moreover, graph generation is a computationally complex task, which necessitates specialized logic solvers.

**Refinery is a novel open-source software framework** to automatically synthesize a diverse set of consistent domain-specific graph models. The framework offers an expressive high-level specification language using partial models to succinctly formulate a wide range of graph generation challenges. It also provides a modern cloud-based architecture for a scalable _Graph Solver as a Service,_ which uses logic reasoning rules to efficiently synthesize a diverse set of solutions to graph generation problems by partial model refinement. Applications include system-level architecture synthesis, test generation for modeling tools or traffic scenario synthesis for autonomous vehicles.
