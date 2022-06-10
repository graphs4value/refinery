import numpy as np
import pandas as pd

method_scores = []
method_commit_scores = []
baseline_scores = []
baseline_commit_scores = []
method_compare = []
baseline_compare = []
user_x_label = []
with open("get_random.txt", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableGetBenchmark.immutableGetBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        method_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.immutableGetAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        method_commit_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.baselineGetBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        baseline_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableGetBenchmark.baselineGetAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        baseline_commit_scores.append(float(xs[6]))
    if int(xs[1]) not in user_x_label:
        user_x_label.append(int(xs[1]))

print('-----------GET-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print(user_x_label)
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
user_x_label = []
with open("getall_random.txt", "r") as a_file:
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
    if int(xs[1]) not in user_x_label:
        user_x_label.append(int(xs[1]))
print('')
print('')
print('')
print('-----------GET ALL-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print(user_x_label)

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
user_x_label = []
with open("put_random", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.immutablePutBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[3]) == 512:
        method_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.immutablePutAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[3]) == 512:
        method_commit_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.baselinePutBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[3]) == 512:
        baseline_scores.append(float(xs[9]))
    if xs[0] == 't.r.s.m.b.put.ImmutablePutBenchmark.baselinePutAndCommitBenchmark' and int(xs[2]) == 1000 and int(xs[6]) == 2 and int(xs[3]) == 512:
        baseline_commit_scores.append(float(xs[9]))
    if int(xs[3]) not in user_x_label:
        user_x_label.append(int(xs[3]))
print('')
print('')
print('')
print('-----------PUT-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print(user_x_label)

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
user_x_label = []
with open("putall_random", "r") as a_file:
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
    if int(xs[4]) not in user_x_label:
        user_x_label.append(int(xs[4]))
print('')
print('')
print('')
print('-----------PUT ALL-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print(user_x_label)

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
user_x_label = []
with open("commit_random2", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableCommitBenchmark.immutableCommitBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        method_scores.append(float(xs[6]))
    if xs[0] == 'ImmutableCommitBenchmark.baselineCommitBenchmark' and int(xs[2]) == 1000 and int(xs[3]) == 2 and int(xs[1]) == 512:
        baseline_scores.append(float(xs[6]))
    if int(xs[1]) not in user_x_label:
        user_x_label.append(int(xs[1]))
print('')
print('')
print('')
print('-----------COMMIT-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print(user_x_label)

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
user_x_label = []
with open("getDiffcursor_random.txt", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.immutableGetDiffCursorBenchmark' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
        method_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.immutableGetDiffCursorAndCommitBenchmark' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
        method_commit_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.baselineGetDiffCursorBenchmark' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
        baseline_scores.append(float(xs[7]))
    if xs[0] == 'ImmutableGetDiffCursorBenchmark.baselineGetDiffCursorAndCommitBenchmark' and int(xs[3]) == 1000 and int(xs[4]) == 2 and int(xs[1]) == 10 and int(xs[2]) == 512:
        baseline_commit_scores.append(float(xs[7]))
    if int(xs[2]) not in user_x_label:
        user_x_label.append(int(xs[2]))
print('')
print('')
print('')
print('-----------DIFFCURSOR-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print(user_x_label)

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
user_x_label = []
with open("restore_random.txt", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.immutableRestoreBenchmark' and int(xs[3]) == 1000 and int(xs[5]) == 2 and int(xs[1]) == 10 and int(xs[4]) == 512:
        method_scores.append(float(xs[8]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.immutableRestoreAndCommitBenchmark' and int(xs[3]) == 1000 and int(xs[5]) == 2 and int(xs[1]) == 10 and int(xs[4]) == 512:
        method_commit_scores.append(float(xs[8]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.baselineRestoreBenchmark' and int(xs[3]) == 1000 and int(xs[5]) == 2 and int(xs[1]) == 10 and int(xs[4]) == 512:
        baseline_scores.append(float(xs[8]))
    if xs[0] == 't.r.s.m.b.restore.ImmutableRestoreBenchmark.baselineRestoreAndCommitBenchmark' and int(xs[3]) == 1000 and int(xs[5]) == 2 and int(xs[1]) == 10 and int(xs[4]) == 512:
        baseline_commit_scores.append(float(xs[8]))
    if int(xs[4]) not in user_x_label:
        user_x_label.append(int(xs[4]))
print('')
print('')
print('')
print('-----------RESTORE-----------')

print(method_scores)
print(method_commit_scores)
print(baseline_scores)
print(baseline_commit_scores)
print(user_x_label)

print('')

for f, b in zip(method_scores, baseline_scores):
    method_compare.append(f / b)
for f, b in zip(method_commit_scores, baseline_commit_scores):
    baseline_compare.append(f / b)

print(method_compare)
print(baseline_compare)