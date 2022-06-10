import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.037, 0.073, 0.123, 0.235, 0.501, 0.988, 1.851, 4.449, 7.365, 15.36, 29.881]
data_get_all = [0.18417684099999998, 0.363387226, 0.70936679, 1.415842324, 2.8776907489999997, 5.612889143, 11.110992596, 22.283904152999998, 44.70532260899999, 90.144211667, 180.09706666699998]
data_put = [0.001, 0.003, 0.009, 0.023, 0.067, 0.202, 0.616, 1.846, 4.553, 10.517, 23.724]
data_put_all = [0.048, 0.064, 0.098, 0.177, 0.355, 0.706, 1.376, 2.691, 5.443, 10.619, 22.47, 44.869, 94.066, 181.748, 404.9]
data_getDiffCursor = [0.649, 1.279, 2.558, 5.093, 10.833, 20.622, 40.714, 80.819, 163.551, 327.468, 649.593]
data_commit = [0.979, 11.359, 17.922, 40.379, 113.227, 343.622, 1372.257]
data_restore = [0.001, 0.002, 0.004, 0.008, 0.015, 0.029, 0.06, 0.116, 0.231]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='Baseline benchmarking (nKeys = 1000, nValues = 2)',
    xlabel='Baseline methods with commit ',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()
