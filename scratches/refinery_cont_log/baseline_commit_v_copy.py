import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = []
data_get_all = []
data_put = []
data_put_all = []
data_getDiffCursor = []
data_commit = []
data_restore = []

data = [1.348/512, 5.612889143/512, 0.162/512, 10.619/512, 27.009/512, 0.397/512, 0.002/512]
methods = ['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore']


fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='Baseline benchmarking continuous version (nKeys = 1000, nValues = 2, n = 512)',
    xlabel='Baseline methods with commit ',
    ylabel='ms/op',
)

ax.set_yscale("log")
plt.bar(methods, data)
plt.show()
