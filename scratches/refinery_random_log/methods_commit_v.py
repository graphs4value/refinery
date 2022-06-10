import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.001, 0.002, 0.003, 0.006, 0.012, 0.023, 0.049, 0.11, 0.176, 0.396, 0.97]
data_get_all = [0.087237717, 0.17871927499999998, 0.349026144, 0.745749599, 1.437605431, 2.8058496120000003, 5.5280841050000005, 11.86228278, 22.335730232, 45.92149272699999, 85.56392666699999]
data_put = [0.003, 0.01, 0.03, 0.076, 0.189, 0.424, 1.053, 2.229, 4.598, 10.672, 20.986]
data_put_all = [0.122, 0.137, 0.205, 0.283, 0.424, 0.756, 1.366, 2.833, 5.677, 12.012, 22.267, 40.335, 87.09, 187.998, 353.574]
data_commit = [0.396, 0.237, 0.299, 6.044, 22.18, 98.751, 18.21]
data_getDiffCursor = [0.002, 0.004, 0.011, 0.016, 0.035, 0.089, 0.131, 0.285, 0.52, 1.134, 2.347]
data_restore = [0.003, 0.005, 0.013, 0.024, 0.047, 0.107, 0.185, 0.346, 0.715]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='VersionedMap benchmarking (nKeys = 1000, nValues = 2)',
    xlabel='Methods with commit',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'],  fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()
