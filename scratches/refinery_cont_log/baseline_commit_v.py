import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.036, 0.073, 0.126, 0.237, 0.471, 1.348, 1.895, 3.624, 7.471, 14.239]
data_get_all = [0.18417684099999998, 0.363387226, 0.70936679, 1.415842324, 2.8776907489999997, 5.612889143, 11.110992596, 22.283904152999998, 44.70532260899999, 90.144211667, 180.09706666699998]
data_put = [0.002, 0.006, 0.016, 0.048, 0.162, 0.591, 1.553, 3.956, 8.189, 16.686]
data_put_all = [0.048, 0.064, 0.098, 0.177, 0.355, 0.706, 1.376, 2.691, 5.443, 10.619, 22.47, 44.869, 94.066, 181.748, 404.9]
data_getDiffCursor = [0.871, 1.69, 3.406, 6.513, 12.889, 27.009, 53.425, 113.033, 234.262, 492.595, 1060.882]
data_commit = [0.979, 11.359, 17.922, 40.379, 113.227, 343.622, 1372.257]
data_restore = [0.001, 0.002, 0.005, 0.01, 0.023, 0.041, 0.095]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='Baseline benchmarking continuous version (nKeys = 1000, nValues = 2)',
    xlabel='Baseline methods with commit ',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()
