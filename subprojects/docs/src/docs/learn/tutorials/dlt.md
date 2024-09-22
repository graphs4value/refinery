---
SPDX-FileCopyrightText: 2024 The Refinery Authors
SPDX-License-Identifier: EPL-2.0
description: Refinery@MODELS2024
sidebar_position: 2
sidebar_label: DLT architectures
---

# Distributed Ledger Technology architecture modeling tutorial

This tutorial has appeared at the [ACM / IEEE 27th International Conference on Model Driven Engineering Languages and Systems (MODELS)](https://conf.researchr.org/details/models-2024/models-2024-tutorials/9/T9-Refinery-Logic-based-Partial-Modeling) on September 22, 2024.

## Metamodeling

### 1. Simple example

```refinery try
class FabricNetwork {
    contains Organization[1..*] organizations
    contains Channel[1..*] channels
}

class Organization {
    contains Host[1..*] hosts
}

class Host {
    contains Node[1..4] nodes
}

class Node.

class Channel {
    Node[1..*] peers
}
```

### 2. Complete metamodel without derived features

```refinery checkpoint=metamodel try
abstract class FabricNetwork {
    contains Organization[1..*] organizations
    contains Channel[1..*] channels
}

class KafkaFabricNetwork extends FabricNetwork.

class RaftFabricNetwork extends FabricNetwork.

class Organization {
    contains Host[1..*] hosts
}

class Host {
    contains Node[1..4] nodes
}

abstract class Node.

class OrderingNode extends Node {
    Channel[1..*] orders opposite orderedBy
}

class EndorsingNode extends Node {
    ChaincodeInstance[1..*] endorses opposite endorsedBy
}

class Channel {
    contains ChaincodeInstance[1..*] chaincodes
    OrderingNode[0..*] orderedBy opposite orders
}

class ChaincodeInstance {
    EndorsingNode[2..*] endorsedBy opposite endorses
}
```

## Partial model fragments

### 3. Consistent partial model fragment

```refinery continue=metamodel try
KafkaFabricNetwork(network).
!exists(KafkaFabricNetwork::new).
!exists(RaftFabricNetwork::new).

default !channels(*, *).
channels(network, Ch1).
channels(network, Ch2).

!exists(Organization::new).
Organization(OrgA).
hosts(OrgA, HA1).
Organization(OrgB).
Organization(OrgC).
```

### 4. Inconsistent partial model fragment

```refinery continue=metamodel try
KafkaFabricNetwork(network).
!exists(KafkaFabricNetwork::new).
!exists(RaftFabricNetwork::new).

!exists(Organization::new).
Organization(OrgA).
hosts(OrgA, HA1).
Organization(OrgB).
hosts(OrgB, HA1).
Organization(OrgC).
```

## Graph queries

### 5. Simple constraint

```refinery continue=metamodel try
error channelInKafkaNetworkWithoutOrderer(c) <->
    channels(n, c),
    KafkaFabricNetwork(n),
    !orderedBy(c, _).
```

### 6. Constraint with auxiliary predicate

```refinery continue checkpoint=constraints try
% Auxiliary predicate for invalidOrdererOrganization
pred ordererOrganization(o) <->
    Organization(o),
    hosts(o, h),
    nodes(h, n),
    OrderingNode(n).

error invalidOrdererOrganization(o) <->
    ordererOrganization(o),
    organizations(n, o),
    !KafkaFabricNetwork(n).
```

### 7. Partial model inconsistent with the constraints

```refinery continue=constraints try
RaftFabricNetwork(network).
!exists(KafkaFabricNetwork::new).
!exists(RaftFabricNetwork::new).

organizations(network, OrgA).
hosts(OrgA, HA).
nodes(HA, NA).
OrderingNode(NA).
```

### 8. Definition of derived features

```refinery continue=constraints checkpoint=derived try
% Auxiliary predicate for derived features
pred peerHelper(Channel c, Host h, Node n) <->
    orders(n, c),
    nodes(h, n)
;
    chaincodes(c, i),
    endorses(n, i),
    nodes(h, n).

pred peer(n, c) <-> peerHelper(c, _, n).

pred participatesIn(o, c) <->
    hosts(o, h),
    peerHelper(c, h, _).

pred collaboratesWith(Organization o1, Organization o2) <->
    o1 != o2,
    participatesIn(o1, c),
    participatesIn(o2, c).
```

## Generating consistent models

### 9. Partial model fragment with derived features

```refinery continue=derived try
KafkaFabricNetwork(network).
!exists(Organization::new).
Organization(OrgA).
Organization(OrgB).
Organization(OrgC).

!collaboratesWith(OrgA, OrgB).
collaboratesWith(OrgA, OrgC).
collaboratesWith(OrgB, OrgC).
```

### 10. Scope declaration to control model size

```refinery continue=derived try
scope node = 15..25,
  Channel += 3,
  FabricNetwork = 1.
```

## Propagation rules

### 11. Simple propagation rule

```refinery continue try
propagation rule collaboratesWithSymmetric(Organization o1, Organization o2) <->
    collaboratesWith(o1, o2)
==>
    collaboratesWith(o2, o1).

propagation rule notCollaboratesWithSymmetric(Organization o1, Organization o2) <->
    !collaboratesWith(o1, o2)
==>
    !collaboratesWith(o2, o1).
```

### 12. Propagation rules for negative pattern

```refinery continue checkpoint=propagation try
propagation rule cannotParticipateIn(Organization o1, Channel c) <->
    participatesIn(o2, c),
    !collaboratesWith(o1, o2),
    o1 != o2
==>
    !participatesIn(o1, c).

propagation rule cannotBePeerOf(Node n, Channel c) <->
    nodes(h, n),
    hosts(o, h),
    !participatesIn(o, c)
==>
    !peer(n, c).

% We don't need to know that n is an EndorsingNode to exdcute this rule.
propagation rule cannotEndorse(Node n, ChaincodeInstance i) <->
    chaincodes(c, i),
    !peer(n, c)
==>
    !endorses(n, i).

% We don't need to know that n is an OrderingNode to exdcute this rule.
propagation rule cannotOrder(Node n, Channel c) <->
    !peer(n, c)
==>
    !orders(n, c).
```

### 13. Partial model fragment to illustrate reasoning

```refinery continue=propagation try
KafkaFabricNetwork(network).
hosts(OrgA, HA).
nodes(HA, NA).
hosts(OrgB, HB).
nodes(HB, NB).
channels(network, Ch1).
chaincodes(Ch1, Ci1).
endorses(NA, Ci1).
```

### 14. Complex propagation rule auxiliary predicates and modality

```refinery continue=propagation try
shadow pred endorsesChaincode(EndorsingNode n, Channel c, ChaincodeInstance i) <->
    chaincodes(c, i),
    endorses(n, i).

shadow pred endorsesMultipleChaincodes(EndorsingNode n, Channel c) <->
    endorsesChaincode(n, c, i1),
    endorsesChaincode(n, c, i2),
    i1 != i2.

propagation rule mustEndorse(EndorsingNode n, ChaincodeInstance i) <->
    peer(n, c),
    !endorsesMultipleChaincodes(n, c),
    chaincodes(c, i),
    may endorses(n, i)
==>
    endorses(n, i).
```

### 15. Partial model fragment to illustrate complex propagation

```refinery continue try
KafkaFabricNetwork(network).
hosts(OrgA, HA).
nodes(HA, NA).
hosts(OrgB, HB).
nodes(HB, NB).
channels(network, Ch1).
chaincodes(Ch1, Ci1).

EndorsingNode(NA).
peer(NA, Ch1).
!chaincodes(Ch1, ChaincodeInstance::new).
```
