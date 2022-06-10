import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.001, 0.002, 0.004, 0.009, 0.016, 0.032, 0.061, 0.12]
data_get_all = [0.102706429, 0.20771884599999998, 0.409126232, 0.858144638, 1.680104375, 3.3236813309999995, 7.13832313, 12.781965008999999, 28.265932763, 51.516891, 102.131678]
data_put = [0.002, 0.009, 0.028, 0.063, 0.123, 0.262, 0.799, 1.402, 2.954, 7.593]
data_put_all = [0.134, 0.313, 0.566, 1.226, 2.338, 4.449, 9.577, 17.874, 38.068, 80.7, 144.348, 311.063, 646.702, 1285.329, 2296.492]
data_getDiffCursor = [0.001, 0.003, 0.006, 0.012, 0.024, 0.05, 0.106, 0.193, 0.401, 0.789, 1.594]
data_commit = [0.396, 0.237, 0.299, 6.044, 22.18, 98.751, 18.21]
data_restore = [0.007, 0.013, 0.027, 0.055, 0.104, 0.218, 0.414]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='VersionedMap benchmarking continuous version (nKeys = 1000, nValues = 2)',
    xlabel='Methods',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()