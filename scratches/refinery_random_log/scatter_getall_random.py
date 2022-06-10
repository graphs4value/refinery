import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

data_get = [0.102706429, 0.20771884599999998, 0.409126232, 0.858144638, 1.680104375, 3.3236813309999995, 7.13832313, 12.781965008999999, 28.265932763, 51.516891, 102.131678]
data_get_baseline = [0.11894627899999999, 0.245099827, 0.47078937199999993, 0.931431714, 1.9033390719999999, 3.7053662409999997, 8.187203168, 16.329623927, 32.6422575, 66.387516333, 114.54411777799999]
data_number = [16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384]

plt.scatter(data_number, data_get, label=f'VersionedMap')
plt.scatter(data_number, data_get_baseline, label=f'native Java')

df = pd.DataFrame({"x": data_number,
                   "y": data_get,
                   "nGetAll": 'VersionedMap'})

df2 = pd.DataFrame({"x": data_number,
                   "y": data_get_baseline,
                   "nGetAll": 'Native Java'})

df_all = df.append(df2)

grid = sns.lmplot(x='x', y='y', hue='nGetAll', data=df_all)
plt.xlabel("nGetAll")
plt.ylabel("ms/op")

plt.title('Comparing VersionedMap getAll() with native Java')
plt.legend()
plt.show()