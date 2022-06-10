import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd


data_get = [0.134, 0.313, 0.566, 1.226, 2.338, 4.449, 9.577, 17.874, 38.068, 80.7, 144.348, 311.063, 646.702, 1285.329, 2296.492]
data_get_baseline = [0.019, 0.034, 0.066, 0.132, 0.244, 0.504, 0.994, 1.976, 5.17, 8.086, 16.015, 33.702, 73.625, 130.75, 292.59]
data_number = [1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384]

plt.scatter(data_number, data_get, label=f'VersionedMap')
plt.scatter(data_number, data_get_baseline, label=f'native Java')

df = pd.DataFrame({"x": data_number,
                   "y": data_get,
                   "nPutAll": 'VersionedMap'})

df2 = pd.DataFrame({"x": data_number,
                   "y": data_get_baseline,
                   "nPutAll": 'Native Java'})

df_all = df.append(df2)

grid = sns.lmplot(x='x', y='y', hue='nPutAll', data=df_all)
plt.xlabel("nPutAll")
plt.ylabel("ms/op")

plt.title('Comparing VersionedMap putAll() with native Java')
plt.legend()
plt.show()