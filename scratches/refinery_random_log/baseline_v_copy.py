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

data = [0.008/512, 3.7053662409999997/512, 0.032/512, 8.086/512, 27.316/512, 11.359/512, 0.006/512]
methods = ['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore']

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='Baseline benchmarking (nKeys = 1000, nValues = 2, n = 512)',
    xlabel='Baseline methods',
    ylabel='ms/op',
)

ax.set_yscale("log")
plt.bar(methods, data)
plt.show()
