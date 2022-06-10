import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

data_get = [0.016, 0.041, 0.093, 0.197, 0.407, 0.9, 2.006, 3.879, 9.186]
data_get_baseline = [0.005, 0.008, 0.016, 0.032, 0.059, 0.111, 0.21, 0.39, 0.806]
data_number = [64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384]

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
                   "nPut": 'VersionedMap'})

df2 = pd.DataFrame({"x": data_number,
                   "y": data_get_baseline,
                   "nPut": 'Native Java'})

df_all = df.append(df2)

# grid = sns.lmplot(x='x', y='y', hue='nPut', data=df_all)
plt.xlabel("nPut")
plt.ylabel("ms/op")

plt.title('Comparing VersionedMap put() with native Java')
plt.legend()
plt.show()