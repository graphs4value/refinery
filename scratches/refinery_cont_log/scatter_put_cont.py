import matplotlib.pyplot as plt
import seaborn as sns
import pandas as pd

data_get = [0.009, 0.028, 0.063, 0.123, 0.262, 0.799, 1.402, 2.954, 7.593]
data_get_baseline = [0.002, 0.005, 0.009, 0.019, 0.038, 0.079, 0.143, 0.286, 0.555]
data_number = [64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384]

data_divided = []
data_divided_baseline = []

for f, b in zip(data_get, data_number):
    data_divided.append(f / b)
for f, b in zip(data_get_baseline, data_number):
    data_divided_baseline.append(f / b)

print(data_divided)
print(data_divided_baseline)

[0.00025, 0.0003203125, 0.00036328125, 0.000384765625, 0.0003974609375, 0.000439453125, 0.00048974609375, 0.0004735107421875, 0.0005606689453125]
[7.8125e-05, 6.25e-05, 6.25e-05, 6.25e-05, 5.76171875e-05, 5.419921875e-05, 5.126953125e-05, 4.7607421875e-05, 4.91943359375e-05]

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

plt.title('Comparing VersionedMap put() with native Java continuous version')
plt.legend()
plt.show()