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
data = [0.023/512, 2.8058496120000003/512, 0.424/512, 12.012/512, 0.089/512, 0.237/512, 0.024/512]
methods = ['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore']

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='VersionedMap benchmarking (nKeys = 1000, nValues = 2, n = 512)',
    xlabel='Methods with commit',
    ylabel='ms/op',
)

ax.set_yscale("log")
plt.bar(methods, data)
plt.show()
