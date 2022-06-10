import numpy as np
import pandas as pd

method_scores = []
with open("private_oopsla", "r") as a_file:
  for line in a_file:
    stripped_line = line.strip()
    xs = [x for x in stripped_line.split(" ") if x != '']
    if xs[0] == 'JmhMapBenchmarks.timeContainsKey' and xs[6] == 'VF_SCALA':
        method_scores.append(xs[9])

    # if xs[0] == 'JmhMapBenchmarks.timeContainsKey' and xs[6] == 'VF_SCALA':
    # if xs[0] == 'JmhMapBenchmarks.timeInsertContained' and xs[6] == 'VF_SCALA':
    # if xs[0] == 'JmhMapBenchmarks.timeIteration' and xs[6] == 'VF_SCALA':
    # if xs[0] == 'JmhMapBenchmarks.timeRemoveKey' and xs[6] == 'VF_SCALA':
    # if xs[0] == 'JmhMapBenchmarks.timeEqualsDeltaDuplicate' and xs[6] == 'VF_SCALA':
    # if xs[0] == 'JmhMapBenchmarks.timeEqualsRealDuplicate' and xs[6] == 'VF_SCALA':
    # if xs[0] == 'JmhMapBenchmarks.timeEntryIteration' and xs[6] == 'VF_SCALA':

    # if xs[0] == 'JmhMapBenchmarks.timeContainsKey' and xs[6] == 'VF_CLOJURE':
    # if xs[0] == 'JmhMapBenchmarks.timeInsertContained' and xs[6] == 'VF_CLOJURE':
    # if xs[0] == 'JmhMapBenchmarks.timeIteration' and xs[6] == 'VF_CLOJURE':
    # if xs[0] == 'JmhMapBenchmarks.timeRemoveKey' and xs[6] == 'VF_CLOJURE':
    # if xs[0] == 'JmhMapBenchmarks.timeEqualsDeltaDuplicate' and xs[6] == 'VF_CLOJURE':
    # if xs[0] == 'JmhMapBenchmarks.timeEqualsRealDuplicate' and xs[6] == 'VF_CLOJURE':
    # if xs[0] == 'JmhMapBenchmarks.timeEntryIteration' and xs[6] == 'VF_CLOJURE':


    if xs[0] == 'JmhMapBenchmarks.timeInsertContained' and xs[6] == 'VF_SCALA':
        if int(xs[5]) > 8 and int(xs[5]) < 32768:
            method_scores.append(xs[9])


    # print(row)
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeContainsKey':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeInsertContained':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeIteration':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeRemoveKey':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeEqualsDeltaDuplicate':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeEqualsRealDuplicate':
    #     print(row[1]['Score'])
    #     method_scores.append(row[1]['Score'])


print(method_scores)
