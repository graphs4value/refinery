import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
# data_get = [0.011]
# data_get_all = [3.3236813309999995]
# data_put = [0.197]
# data_put_all = [80.7]
# data_getDiffCursor = [0.053]
# data_commit = [0.237]
# data_restore = [0.018]
# data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]
data = [0.011/512, 3.3236813309999995/512, 0.197/512, 80.7/512, 0.053/512, 0.237/512, 0.018/512]
print(data)

methods = ['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore']

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()
#
ax.set(
    axisbelow=True,
    title='VersionedMap benchmarking (nKeys = 1000, nValues = 2, n=512)',
    xlabel='Methods',
    ylabel='ms/op',
)
#
# ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.bar(methods, data)
plt.show()