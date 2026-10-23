# Assignment 3: Graph Algorithms

Java 17 implementations of DSU, Kruskal, Prim, Kosaraju SCC, DFS topological
ordering and DAG shortest paths, with route reconstruction.

## Run and test

Requires JDK 17+ and Python 3.12+. Run from the repository root:

```sh
python3 tools/run.py --setup
python3 tools/run.py --check
python3 tools/run.py --input tests/my_cases.jsonl
python3 tests/check.py --all
python3 tests/benchmark.py
```

Use `python` or `py -3` if needed on your system. No external packages are required.

The program reads one UTF-8 JSON request per line and writes one JSON result per line.
The implementation is in [src/Java/Algorithms.java](src/Java/Algorithms.java).
[ANALYSIS.md](ANALYSIS.md) explains the algorithms, correctness, complexity and benchmark.
[tests/README.md](tests/README.md) describes the tests and stored results.
