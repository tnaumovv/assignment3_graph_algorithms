"""Compile first, then measure the Java methods inside one JVM."""
import json
from pathlib import Path
import platform
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import run


def main():
    command = run.build('Java')
    output = ROOT / '.build' / 'Java'
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-cp', str(output),
                    '-d', str(output), str(ROOT / 'tests' / 'Benchmark.java')], check=True, timeout=60)
    java_version = subprocess.run(['java', '-version'], capture_output=True, text=True, check=True)
    hardware = platform.processor() or platform.machine()
    if sys.platform == 'darwin':
        hardware = subprocess.check_output(['sysctl', '-n', 'machdep.cpu.brand_string'], text=True).strip()
    completed = subprocess.run(command[:-1] + ['Benchmark'], capture_output=True, text=True,
                               encoding='utf-8', check=True, timeout=120)
    rows = [json.loads(line) for line in completed.stdout.splitlines()]
    assert len(rows) == 3
    for row in rows:
        assert row['all_forests_verified']
        for method in ('kruskal', 'prim'):
            samples = row[method + '_ms']
            assert len(samples) >= 3
            assert sorted(samples)[len(samples) // 2] == row[method + '_median_ms']
    result = {
        'runtime': java_version.stderr.strip(), 'os': platform.platform(),
        'hardware': hardware, 'python': platform.python_version(),
        'command': 'python3 tests/benchmark.py', 'jvm_options': '-Xmx384m',
        'timer': 'System.nanoTime; elapsed nanoseconds divided by 1000000',
        'warmup': '5 untimed calls per method per graph, after one verified initial call',
        'timing_boundary': 'Each complete Algorithms method, including Input.edges copying, '
                           'algorithm-specific graph construction, sorting/heap work and result creation. '
                           'Excludes compilation, JVM launch, input generation, hashing, JSON parsing/printing '
                           'and forest verification. Automatic garbage collection can occur inside a sample.',
        'ordering': 'The method run first alternates between measurement rounds.',
        'generator': 'java.util.Random(20261023 + n); negative weighted chain plus nonnegative extra '
                     'edges, then Collections.shuffle with the same Random; see tests/Benchmark.java.',
        'rows': rows,
    }
    destination = ROOT / 'tests' / 'results' / 'benchmark.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print('n,m,Kruskal median ms,Prim median ms')
    for row in rows:
        print(f'{row["n"]},{row["m"]},{row["kruskal_median_ms"]:.6f},{row["prim_median_ms"]:.6f}')


if __name__ == '__main__':
    main()
