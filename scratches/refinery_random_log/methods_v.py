import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.001, 0.001, 0.003, 0.006, 0.011, 0.024, 0.046, 0.088, 0.179, 0.381]
data_get_all = [0.102706429, 0.20771884599999998, 0.409126232, 0.858144638, 1.680104375, 3.3236813309999995, 7.13832313, 12.781965008999999, 28.265932763, 51.516891, 102.131678]
data_put = [0.002, 0.006, 0.016, 0.041, 0.093, 0.197, 0.407, 0.9, 2.006, 3.879, 9.186]
data_put_all = [0.134, 0.313, 0.566, 1.226, 2.338, 4.449, 9.577, 17.874, 38.068, 80.7, 144.348, 311.063, 646.702, 1285.329, 2296.492]
data_getDiffCursor = [0.002, 0.003, 0.007, 0.015, 0.027, 0.053, 0.113, 0.217, 0.424, 0.89, 1.714]
data_commit = [0.396, 0.237, 0.299, 6.044, 22.18, 98.751, 18.21]
data_restore = [0.002, 0.005, 0.008, 0.018, 0.034, 0.061, 0.112, 0.216, 0.452]
data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='VersionedMap benchmarking (nKeys = 1000, nValues = 2)',
    xlabel='Methods',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()