import numpy as np
import pandas as pd

method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
with open("get_cont", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableGetBenchmark.immutableGetBenchmarkContinuous':
        method_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.immutableGetAndCommitBenchmarkContinuous':
        method_commit_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.baselineGetBenchmarkContinuous':
        baseline_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.baselineGetAndCommitBenchmarkContinuous':
        baseline_commit_scores.append(float(xs[6]))

print('-----------GET-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)
for f, b in zip(method_commit_scores, baseline_commit_scores):
    baseline_compare.append(f / b)

print(method_compare)
print(baseline_compare)


method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
with open("getall_cont.txt", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableGetAllBenchmark.immutableGetAllBenchmark':
        method_scores.append(float(xs[6]) * (10 ** -6))
    if xs[0] == 'ImmutableGetAllBenchmark.immutableGetAllAndCommitBenchmark':
        method_commit_scores.append(float(xs[6]) * (10 ** -6))
    if xs[0] == 'ImmutableGetAllBenchmark.baselineGetAllValuesBenchmark':
        baseline_scores.append(float(xs[6]) * (10 ** -6))
    if xs[0] == 'ImmutableGetAllBenchmark.baselineGetAllValuesAndCommitBenchmark':
        baseline_commit_scores.append(float(xs[6]) * (10 ** -6))

print('')
print('')
print('')
print('-----------GET ALL-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)
for f, b in zip(method_commit_scores, baseline_commit_scores):
    baseline_compare.append(f / b)

print(method_compare)
print(baseline_compare)

method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
with open("put_cont", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.immutablePutBenchmarkContinuous':
        method_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.immutablePutAndCommitBenchmarkContinuous':
        method_commit_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.baselinePutBenchmarkContinuous':
        baseline_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.baselinePutAndCommitBenchmarkContinuous':
        baseline_commit_scores.append(float(xs[9]))

print('')
print('')
print('')
print('-----------PUT-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)
for f, b in zip(method_commit_scores, baseline_commit_scores):
    baseline_compare.append(f / b)

print(method_compare)
print(baseline_compare)

method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
with open("putall_cont.txt", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.immutablePutAllBenchmark':
        method_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.immutablePutAllAndCommitBenchmark':
        method_commit_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.baselinePutAllBenchmark':
        baseline_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.baselinePutAllAndCommitBenchmark':
        baseline_commit_scores.append(float(xs[9]))

print('')
print('')
print('')
print('-----------PUT ALL-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)
for f, b in zip(method_commit_scores, baseline_commit_scores):
    baseline_compare.append(f / b)

print(method_compare)
print(baseline_compare)

method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
with open("commit_cont", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 't.r.s.m.b.commit.ImmutableCommitBenchmark.immutableCommitBenchmark':
        method_scores.append(float(xs[8]))
    if xs[0] == 't.r.s.m.b.commit.ImmutableCommitBenchmark.baselineCommitBenchmark':
        baseline_scores.append(float(xs[8]))

with open("commit_cont2", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableCommitBenchmark.immutableCommitBenchmark':
        method_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableCommitBenchmark.baselineCommitBenchmark':
        baseline_scores.append(float(xs[6]))

print('')
print('')
print('')
print('-----------COMMIT-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)

print(method_compare)
print(baseline_compare)

method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
with open("getDiffcursor_cont", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.immutableGetDiffCursorBenchmarkContinuous':
        method_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.immutableGetDiffCursorAndCommitBenchmarkContinuous':
        method_commit_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.baselineGetDiffCursorBenchmarkContinuous':
        baseline_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.baselineGetDiffCursorAndCommitBenchmarkContinuous':
        baseline_commit_scores.append(float(xs[7]))

print('')
print('')
print('')
print('-----------DIFFCURSOR-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)
for f, b in zip(method_commit_scores, baseline_commit_scores):
    baseline_compare.append(f / b)

print(method_compare)
print(baseline_compare)

method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
with open("restore_cont", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.immutableRestoreBenchmark':
        method_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.immutableRestoreAndCommitBenchmark':
        method_commit_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.baselineRestoreBenchmark':
        baseline_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.baselineRestoreAndCommitBenchmark':
        baseline_commit_scores.append(float(xs[9]))

print('')
print('')
print('')
print('-----------RESTORE-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)
for f, b in zip(method_commit_scores, baseline_commit_scores):
    baseline_compare.append(f / b)

print(method_compare)
print(baseline_compare)