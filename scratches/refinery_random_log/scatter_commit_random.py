import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

data_get = [0.396, 0.237, 0.299, 6.044, 22.18, 98.751, 18.21]
data_get_baseline = [0.979, 11.359, 17.922, 40.379, 113.227, 343.622, 1372.257]
data_number = [32, 512, 1024, 2048, 4096, 8192, 16384]

plt.scatter(data_number, data_get, label=f'VersionedMap')
plt.scatter(data_number, data_get_baseline, label=f'native Java')

df = pd.DataFrame({"x": data_number,
                   "y": data_get,
                   "nCommit": 'VersionedMap'})

df2 = pd.DataFrame({"x": data_number,
                   "y": data_get_baseline,
                   "nCommit": 'Native Java'})

df_all = df.append(df2)

grid = sns.lmplot(x='x', y='y', hue='nCommit', data=df_all)
plt.xlabel("nCommit")
plt.ylabel("ms/op")

plt.title('Comparing VersionedMap commit() with native Java')
plt.legend()
plt.show()