import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.001, 0.001, 0.003, 0.006, 0.014, 0.022, 0.048, 0.091]
data_get_all = [0.11894627899999999, 0.245099827, 0.47078937199999993, 0.931431714, 1.9033390719999999, 3.7053662409999997, 8.187203168, 16.329623927, 32.6422575, 66.387516333, 114.54411777799999]
data_put = [0.001, 0.002, 0.005, 0.009, 0.019, 0.038, 0.079, 0.143, 0.286, 0.555]
data_put_all = [0.019, 0.034, 0.066, 0.132, 0.244, 0.504, 0.994, 1.976, 5.17, 8.086, 16.015, 33.702, 73.625, 130.75, 292.59]
data_getDiffCursor = [0.642, 1.289, 2.563, 5.16, 10.194, 20.574, 41.035, 82.607, 166.041, 332.986, 1179.585]
data_commit = [0.979, 11.359, 17.922, 40.379, 113.227, 343.622, 1372.257]
data_restore = [0.001, 0.001, 0.002, 0.005, 0.009, 0.019, 0.038]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='Baseline benchmarking continuous version (nKeys = 1000, nValues = 2)',
    xlabel='Baseline methods',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()
