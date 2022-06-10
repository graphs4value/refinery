import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

data_get = [0.002, 0.003, 0.007, 0.015, 0.027, 0.053, 0.113, 0.217, 0.424, 0.89, 1.714]
data_get_baseline = [0.97, 1.649, 4.293, 11.347, 14.295, 27.316, 57.257, 112.053, 255.931, 496.865, 678.946]
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

plt.title('Comparing VersionedMap getDiffCursor() with native Java')
plt.legend()
plt.show()