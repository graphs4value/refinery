import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

data_get = [0.001, 0.003, 0.006, 0.012, 0.024, 0.05, 0.106, 0.193, 0.401, 0.789, 1.594]
data_get_baseline = [0.642, 1.289, 2.563, 5.16, 10.194, 20.574, 41.035, 82.607, 166.041, 332.986, 1179.585]
data_number = [16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384]

plt.scatter(data_number, data_get, label=f'VersionedMap')
plt.scatter(data_number, data_get_baseline, label=f'native Java')

df = pd.DataFrame({"x": data_number,
                   "y": data_get,
                   "nGetDiffCursor": 'VersionedMap'})

df2 = pd.DataFrame({"x": data_number,
                   "y": data_get_baseline,
                   "nGetDiffCursor": 'Native Java'})

df_all = df.append(df2)
grid = sns.lmplot(x='x', y='y', hue='nGetDiffCursor', data=df_all)
plt.xlabel("nGetDiffCursor")
plt.ylabel("ms/op")
plt.title('Comparing VersionedMap getDiffCursor() with native Java continuous version')
plt.legend()
plt.show()