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
    if xs[0] == 'ImmutableGetBenchmark.immutableGetBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        method_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.immutableGetAndCommitBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        method_commit_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.baselineGetBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        baseline_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.baselineGetAndCommitBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
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
    if xs[0] == 'ImmutableGetAllBenchmark.immutableGetAllBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        method_scores.append(float(xs[6]) * (10 ** -6))
    if xs[0] == 'ImmutableGetAllBenchmark.immutableGetAllAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        method_commit_scores.append(float(xs[6]) * (10 ** -6))
    if xs[0] == 'ImmutableGetAllBenchmark.baselineGetAllValuesBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        baseline_scores.append(float(xs[6]) * (10 ** -6))
    if xs[0] == 'ImmutableGetAllBenchmark.baselineGetAllValuesAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
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
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.immutablePutBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[4]) == 2 and int(xs[3]) == 512:
        method_scores.append(float(xs[7]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.immutablePutAndCommitBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[4]) == 2 and int(xs[3]) == 512:
        method_commit_scores.append(float(xs[7]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.baselinePutBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[4]) == 2 and int(xs[3]) == 512:
        baseline_scores.append(float(xs[7]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.baselinePutAndCommitBenchmarkContinuous' and int(xs[2]) == 1000 and int(xs[4]) == 2 and int(xs[3]) == 512:
        baseline_commit_scores.append(float(xs[7]))

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
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.immutablePutAllBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[4]) == 512:
        method_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.immutablePutAllAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[4]) == 512:
        method_commit_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.baselinePutAllBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[4]) == 512:
        baseline_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.putall.ImmutablePutAllBenchmark.baselinePutAllAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[4]) == 512:
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
# with open("commit_cont", "r") as a_file:
#   for line in a_file:
#     stripped_line = line.strip()
#     xs = [x for x in stripped_line.split(" ") if x != '']
#     if xs[0] == 't.r.s.m.b.commit.ImmutableCommitBenchmark.immutableCommitBenchmark' and int(xs[4]) == 32 and int(xs[1]) == 512:
#         method_scores.append(float(xs[8]))
#     if xs[0] == 't.r.s.m.b.commit.ImmutableCommitBenchmark.baselineCommitBenchmark' and int(xs[4]) == 32 and int(xs[1]) == 512:
#         baseline_scores.append(float(xs[8]))

with open("commit_cont2", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableCommitBenchmark.immutableCommitBenchmark' and int(xs[2]) == 32 and int(xs[1]) == 512:
        method_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableCommitBenchmark.baselineCommitBenchmark' and int(xs[2]) == 32 and int(xs[1]) == 512:
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
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.immutableGetDiffCursorBenchmarkContinuous' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
        method_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.immutableGetDiffCursorAndCommitBenchmarkContinuous' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
        method_commit_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.baselineGetDiffCursorBenchmarkContinuous' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
        baseline_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.baselineGetDiffCursorAndCommitBenchmarkContinuous' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
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
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.immutableRestoreBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[1]) == 10 and int(xs[5]) == 512:
        method_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.immutableRestoreAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[1]) == 10 and int(xs[5]) == 512:
        method_commit_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.baselineRestoreBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[1]) == 10 and int(xs[5]) == 512:
        baseline_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.baselineRestoreAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[1]) == 10 and int(xs[5]) == 512:
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