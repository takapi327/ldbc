import sys
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import json
import glob

def process_jmh_results(file_paths):
    data = []
    for file_path in file_paths:
        with open(file_path, 'r') as f:
            results = json.load(f)
            for result in results:
                benchmark = result['benchmark'].split('.')[-1]
                params = result['params']
                score = result['primaryMetric']['score']
                # パラメータを個別の列として追加
                for param, value in params.items():
                    data.append({
                        'benchmark': benchmark,
                        'param': param,
                        'value': int(value),
                        'score': score
                    })
    return pd.DataFrame(data)

def plot_results(df, output):
    sns.set_theme()
    g = sns.relplot(
        data=df,
        kind="line",
        x="value",
        y="score",
        hue="benchmark",
        style="benchmark",
        dashes=False,
        markers=True
    )
    g.set_axis_labels("Record size", "Throughput (ops/s)")
    g.legend.set(title=None)
    g.tight_layout()
    g.savefig(output, format='svg')

if __name__ == "__main__":
    input_pattern = sys.argv[1]
    output = sys.argv[2]

    file_paths = glob.glob(input_pattern)
    df = process_jmh_results(file_paths)
    plot_results(df, output)
