# Assignment 3: Graph Algorithms

Java 17 solution for Design and Analysis of Algorithms.

The program supports `dsu`, `kruskal`, `prim`, `scc`, `topo` and `dag`.
It reads UTF-8 JSON Lines from standard input and writes one JSON object per request.
The algorithms use only standard Java classes. No downloaded libraries are needed.

## Requirements

- JDK 17 or newer, with `java` and `javac` on PATH.
- Python 3.12 or newer for the supplied launcher and test scripts.
- `submission.json` selects `Java`.

## Build, run and test

Run commands from the repository root:

```sh
python3 tools/run.py --setup
python3 tools/run.py --check
python3 tools/run.py --input tests/my_cases.jsonl
python3 tests/check.py --all
python3 tests/benchmark.py
```

On systems where Python is named `python`, use `python` instead of `python3`.
On Windows, `py -3` is another option.

Build and run without the Python launcher:

```sh
mkdir -p .build/Java
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java
java -Xmx384m -cp .build/Java Main < tests/my_cases.jsonl
```

`--setup` checks ping only. `--check` checks the 36 public cases and request-state reset.
The own checks also exercise alternative answers, route reconstruction, independent
small-graph oracles and full limits. See [tests/README.md](tests/README.md) for details.
