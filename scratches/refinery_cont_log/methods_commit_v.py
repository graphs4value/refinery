import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.001, 0.001, 0.002, 0.003, 0.007, 0.018, 0.027, 0.064, 0.105, 0.217, 0.484]
data_get_all = [0.087237717, 0.17871927499999998, 0.349026144, 0.745749599, 1.437605431, 2.8058496120000003, 5.5280841050000005, 11.86228278, 22.335730232, 45.92149272699999, 85.56392666699999]
data_put = [0.004, 0.015, 0.05, 0.13, 0.309, 0.714, 1.574, 3.302, 7.109, 14.163]
data_put_all = [0.122, 0.137, 0.205, 0.283, 0.424, 0.756, 1.366, 2.833, 5.677, 12.012, 22.267, 40.335, 87.09, 187.998, 353.574]
data_commit = [0.396, 0.237, 0.299, 6.044, 22.18, 98.751, 18.21]
data_getDiffCursor = [0.002, 0.004, 0.009, 0.016, 0.036, 0.06, 0.147, 0.242, 0.569, 0.993, 2.233]
data_restore = [0.015, 0.025, 0.052, 0.127, 0.224, 0.417, 0.85]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='VersionedMap benchmarking continuous version (nKeys = 1000, nValues = 2)',
    xlabel='Methods with commit',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'],  fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()
