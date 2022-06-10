import numpy as np
import pandas as pd

df = pd.read_csv("results.all-20150817_0732.log", parse_dates=True, sep=',')
method_scores = []

for row in df.iterrows():
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeEqualsRealDuplicate':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeContainsKey':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeInsertContained':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeIteration':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeRemoveKey':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeEqualsDeltaDuplicate':

    # if row[1]['Param: valueFactoryFactory'] == 'VF_CLOJURE' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeContainsKey':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_CLOJURE' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeInsertContained':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_CLOJURE' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeIteration':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_CLOJURE' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeRemoveKey':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_CLOJURE' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeEqualsDeltaDuplicate':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_CLOJURE' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeEqualsRealDuplicate':
    # if row[1]['Param: valueFactoryFactory'] == 'VF_CLOJURE' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeEntryIteration':
    if row[1]['Param: valueFactoryFactory'] == 'VF_SCALA' and row[1]['Benchmark'] == 'nl.cwi.swat.jmh_dscg_benchmarks.JmhMapBenchmarks.timeInsertContained':
        if row[1]['Param: size'] > 8 and row[1]['Param: size'] < 32768 and row[1]['Param: run'] == 4:
            method_scores.append(row[1]['Score'])

print(method_scores)

# print(row[1]['Score'])
# print(row)

