---
SPDX-FileCopyrightText: 2023-2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
description: Introduction to classes, references, and error predicates
sidebar_position: 0
sidebar_label: File system
---

# File system tutorial

The goal of this tutorial is to give a brief overview of the partial modeling and model generation features of the Refinery framework. The running example will be the modeling of files, directories, and repositories.

## Partial models

### Types and relations

- First, let us introduce some basic types: `Dir`, `File`, and `FileSystem`, along with the relations between them: `element` and `root`. There is a `scope` expression at the end, which we will ignore for now.

```refinery
class FileSystem {
    contains File[1] root
}

class File.

class Dir extends File {
    contains File[] element
}

scope node = 10.
```

import Link from '@docusaurus/Link';

<p>
  <Link
    href="https://refinery.services/#/1/KLUv_SDT_QMAQkcXGnBL2-ikxOa10ZNeN1bwnxijfsojpwHQAxAE5pzBk5uCd8F5EjAGJrUNQBWIbdRU7tkRB-VsG_aVuMlSEWzzTShXE8h-eBHzK_cK11NoD9P_2_GFrS61RRmuipYUCwA046ljtvEqgDAGQyDQwsIqKACEt2LiANXAaUxBAQ=="
    className="button button--lg button--primary button--play"
  >Try in Refinery</Link>
</p>

- Notice that the syntax is essentially identical to [Xcore](https://wiki.eclipse.org/Xcore).
- Review the partial model visualization. You should get something like this:

import Fig1 from './fig1.svg';

<Fig1 title="Initial model" />

- Add some statements about a partial model:

```refinery
class FileSystem {
    contains File[1] root
}

class File.

class Dir extends File {
    contains File[] element
}

Dir(resources).
element(resources, img).
File(img).

scope node = 10.
```

import Fig2 from './fig2.svg';

<Fig2 title="Partial model extended with new facts" />

### Partial models

- Notice that the instance model elements are coexisting with ```<type>::new``` nodes representing the prototypes of newly created objects.

- Check the disabled `equals` and `exist` predicates. check the visual annotation of those predicates in the visualization (dashed line, shadow).

import Fig3 from './fig3.svg';

<Fig3 title="Object existence and equality" />

### Information merging

- For the object `img`, we didn't specify if it is a directory or not. Therefore, it will typically be a folder.

- If we want to state that img is not a directory, we need to a negative statement:

```refinery
!Dir(img).
```

- Statements are merged with respect to the refinement relation of 4-valued logic.

- If we add, a statement both negatively and positively, it will create an inconsistency:

```refinery
element(resources, img).
!element(resources, img).
```

- Inconsistent models parts in a partial model typically make the problem trivially unsatisfiable.

import Fig4 from './fig4.svg';

<Fig4 title="Inconsistent partial model" />

- However, the model can be saved if the inconsistent part may not exist...

```refinery
!File(File::new).
```

### Default values

- A large amount of statements can be expressed by using `*`.
- The `default` keyword defines lower priority statements that need to be considered unless other statement specifies otherwise. No information merging is happening.

## Constraints

Let's extend the metamodel with a new class `SymLink`:

```refinery
class FileSystem {
    contains File[1] root
}

class File.

class Dir extends File {
    contains File[0..10] element
}

class SymLink extends File {
    File[1] target
}

Dir(resources).
element(resources, img).
element(resources, link).
target(link, img).

scope node = 10.
```

- Add some simple constraints:

```refinery
% Simple constraints:
pred hasReference(f) <-> target(_, f).
error pred selfLoop(s) <-> target(s, s).
target(x,x).
```

- There are no empty directories in a git repository, so let's forbid them!

```refinery
error pred emptyDir(d) <-> Dir(d), !element(d,_).
```

- End result:

```refinery
class FileSystem {
    contains File[1] root
}

class File.

class Dir extends File {
    contains File[0..10] element
}

class SymLink extends File {
    File[1] target
}

Dir(resources).
element(resources, img).
!Dir(img).
element(resources, link).
target(link,img).

% Simple constraints:
pred hasReference(f) <-> target(_, f).
error pred selfLoop(s) <-> target(s, s).

% Object equality with ==:
error pred emptyDir(d) <-> Dir(d), !element(d, _).
pred importantFile(f) <-> target(l1, f), target(l2, f), l1 != l2.

% Transitive closure, and
pred containsFile(fs, file) <->
    FileSystem(fs),
    root(fs, file)
;
    FileSystem(fs),
    root(fs, rootDir),
    element+(rootDir, file).

% Predicate reuse
error conflictBetweenTwoFileSystem(fs1, fs2, l, t) <->
    containsFile(fs1, l),
    containsFile(fs2, t),
    fs1 != fs2,
    target(l, t).

scope node = 40..50, FileSystem = 2, importantFile = 1..*.
```
