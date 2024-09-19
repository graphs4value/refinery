---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
description: Refinery@FAME2024
sidebar_position: 1
sidebar_label: Project planning
---

# Project tutorial

This tutorial has appeared at the [1st International School on Foundations and Advances of Model-Based Engineering (FAME)](https://fame-school.github.io/) on September 20, 2024.

## Metamodeling

### 1. Metamodel from the MDE Hands-on

```refinery try
class Project {
    contains Task[] tasks
    contains Person[] people
}

class Task {
    contains Effort[1..*] effort
}

class Person.

class Effort {
    Person[1] person
}
```

### 2. Metamodel extended with dependencies and teams

```refinery checkpoint=metamodel try
class Project {
    contains Task[] tasks
    contains Team[] teams
}

class Task {
    contains Effort[1..*] effort
    Task[] dependsOn
}

class Team {
    contains Person[1..*] people
}

class Person {
    Effort[0..*] effort opposite person
}

class Effort {
    Person[1] person opposite effort
}
```

## 3. Partial modeling

```refinery continue=metamodel checkpoint=instance try
Project(proj).
!exists(Project::new).
tasks(proj, task1).
tasks(proj, task2).
scope Task += 0.
default !dependsOn(*, *).
dependsOn(task2, task1).
teams(proj, team1).
teams(proj, team2).
people(team1, alice).
people(team1, bob).
!people(team1, Person::new).
Person(carol).
?exists(team2).
!exists(Team::new).
```

## Model generation

### 4. Object scopes

Add scope constraints to the [metamodel](#metamodel-extended-with-dependencies-and-teams).

```refinery continue=metamodel checkpoint=scope try
scope node = 30..50, Person += 10, Task += 5, Project = 1, Team = 3.
```

### 5. From existing partial model

Add scope constraints to the [initial partial model](#partial-modeling).

```refinery continue=instance try
scope node = 15, Project = 1, Team = 2.
```

## Graph queries

### 6. Error predicates

```refinery continue=scope checkpoint=error try
error repeatedEffort(Effort effort1, Effort effort2) <->
    effort1 != effort2,
    person(effort1, person),
    person(effort2, person),
    Task::effort(task, effort1),
    Task::effort(task, effort2).

error cyclicDependency(Task task) <->
    dependsOn+(task, task).
```

### 7. Highlight errors in model

```refinery continue=error try
tasks(proj, task1).
tasks(proj, task2).
dependsOn(task1, task2).
dependsOn(task2, task1).

teams(proj, team1).
people(team1, alice).
Task::effort(task1, effort1).
person(effort1, alice).
Task::effort(task1, effort2).
person(effort2, alice).
```

### 8. Error predicate with helper predicate

```refinery continue=error try
pred worksOn(Person person, Task task) <->
    Person::effort(person, work),
    Task::effort(task, work).

error taskSharing(Team g1, Team g2) <->
    g1 != g2,
    people(g1, p1),
    people(g2, p2),
    worksOn(p1, t),
    worksOn(p2, t).
```

### 9. Model query for analysis

```refinery continue try
pred communicates(Team g1, Team g2) <->
    people(g1, p1),
    people(g2, p2),
    worksOn(p1, t1),
    worksOn(p2, t2),
    dependsOn(t2, t1).
```
