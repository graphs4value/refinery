import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

data_get = [0.001, 0.002, 0.004, 0.009, 0.016, 0.032, 0.061, 0.12]
data_get_baseline = [0.001, 0.001, 0.003, 0.006, 0.014, 0.022, 0.048, 0.091]
data_number = [128, 256, 512, 1024, 2048, 4096, 8192, 16384]

data_divided = []
data_divided_baseline = []

for f, b in zip(data_get, data_number):
    data_divided.append(f / b)
for f, b in zip(data_get_baseline, data_number):
    data_divided_baseline.append(f / b)

print(data_divided)
print(data_divided_baseline)

fig, ax = plt.subplots(figsize = (9, 6))
# ax.scatter(x, y, s=60, alpha=0.7, edgecolors="k")

# Set logarithmic scale on the both variables
ax.set_xscale("log")

plt.scatter(data_number, data_divided, label=f'VersionedMap')
plt.scatter(data_number, data_divided_baseline, label=f'native Java')

df = pd.DataFrame({"x": data_number,
                   "y": data_get,
                   "nGet": 'VersionedMap'})

df2 = pd.DataFrame({"x": data_number,
                   "y": data_get_baseline,
                   "nGet": 'Native Java'})

df_all = df.append(df2)

# grid = sns.lmplot(x='x', y='y', hue='nGet', data=df_all)
plt.xlabel("nGet")
plt.ylabel("ms/op")

plt.title('Comparing VersionedMap get() with native Java continuous version')
plt.legend()
plt.show()