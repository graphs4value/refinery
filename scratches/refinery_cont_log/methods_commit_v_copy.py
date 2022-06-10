import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = []
data_get_all = []
data_put = []
data_put_all = []
data_commit = []
data_getDiffCursor = []
data_restore = []

# data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]
data = [0.018/512, 2.8058496120000003/512, 0.309/512, 12.012/512, 0.06/512, 0.139/512, 0.025/512]
methods = ['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore']


fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='VersionedMap benchmarking continuous version (nKeys = 1000, nValues = 2, n = 512)',
    xlabel='Methods with commit',
    ylabel='ms/op',
)

ax.set_yscale("log")
plt.bar(methods, data)
plt.show()
