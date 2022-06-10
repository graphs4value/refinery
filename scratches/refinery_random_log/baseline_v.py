import matplotlib.pyplot as plt
import numpy as np

np.random.seed(10)
data_get = [0.001, 0.001, 0.002, 0.004, 0.008, 0.018, 0.034, 0.08, 0.17, 0.285]
data_get_all = [0.11894627899999999, 0.245099827, 0.47078937199999993, 0.931431714, 1.9033390719999999, 3.7053662409999997, 8.187203168, 16.329623927, 32.6422575, 66.387516333, 114.54411777799999]
data_put = [0.001, 0.002, 0.005, 0.008, 0.016, 0.032, 0.059, 0.111, 0.21, 0.39, 0.806]
data_put_all = [0.019, 0.034, 0.066, 0.132, 0.244, 0.504, 0.994, 1.976, 5.17, 8.086, 16.015, 33.702, 73.625, 130.75, 292.59]
data_getDiffCursor = [0.97, 1.649, 4.293, 11.347, 14.295, 27.316, 57.257, 112.053, 255.931, 496.865, 678.946]
data_commit = [0.979, 11.359, 17.922, 40.379, 113.227, 343.622, 1372.257]
data_restore = [0.001, 0.002, 0.003, 0.006, 0.012, 0.025, 0.05, 0.099, 0.207]

data = [data_get, data_get_all, data_put, data_put_all, data_getDiffCursor, data_commit, data_restore]

fig = plt.figure(figsize =(10, 7))
ax = fig.add_subplot()

ax.set(
    axisbelow=True,
    title='Baseline benchmarking (nKeys = 1000, nValues = 2)',
    xlabel='Baseline methods',
    ylabel='ms/op',
)

ax.set_xticklabels(['get', 'getAll', 'put', 'putAll', 'getDiffCursor', 'commit', 'restore'], fontsize=10)
ax.set_yscale("log")
plt.boxplot(data, showfliers=False)

plt.show()
