import sys
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

feature = sys.argv[1]
data_file = sys.argv[2]
output = sys.argv[3]

target_names = {
    'ldbc': 'ldbc',
    'jdbc': 'jdbc',
}

plot_config = {
    'Insert': {
        'xlabel': 'Record size',
        'ylabel': 'Throughput (ops/s)',
        'xmin': 0,
        'ymin': 20,
        'xstep': 1000,
        'ystep': 10,
    },
    'Batch': {
        'xlabel': 'Record size',
        'ylabel': 'Throughput (ops/s)',
        'xmin': 0,
        'ymin': 20,
        'xstep': 1000,
        'ystep': 10,
    },
    'Select': {
        'xlabel': 'Record size',
        'ylabel': 'Throughput (ops/s)',
        'xmin': 0,
        'ymin': 20,
        'xstep': 1000,
        'ystep': 10,
    },
}

def ticks(min, max, step):
    min = int(min * 10)
    max = int(max * 10)
    step = int(step * 10)
    return list(map(lambda n: n / 10, range(min, int(max / step) * step + 1, step)))

conf = plot_config[feature]

df = pd.read_json(data_file)
df['target'] = df['target'].map(target_names)

x = 'index'
y = 'score'
xmax = df[x].max()
ymax = df[y].max()
xmin, xstep = conf['xmin'], conf['xstep']
ymin, ystep = conf['ymin'], conf['ystep']

xticks = ticks(xmin, xmax, xstep)
yticks = ticks(ymin, ymax, ystep)

sns.set_theme()
g = sns.relplot(
    data=df,
    estimator=conf.get('estimator', "average"),
    kind="line",
    x=x,
    y=y,
    hue="target",
    style="target",
    dashes=False,
    markers=True
)
g.set_axis_labels(conf['xlabel'], conf['ylabel'])
g.ax.set_xticks(xticks)
g.ax.set_yticks(yticks)
g.legend.set(title=None)
g.tight_layout()
g.savefig(output, format='svg')
