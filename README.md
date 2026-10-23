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

## Files

- [src/Java/Algorithms.java](src/Java/Algorithms.java): six algorithms and `reconstructRoute`.
- [ANALYSIS.md](ANALYSIS.md): steps, correctness, complexity, tests and benchmark measurements.
- [tests/cases.json](tests/cases.json): 33 cases with justified expected answers.
- [tests/results/](tests/results/): actual outputs, execution logs and measurements.
- [docs/SPECIFICATION_EN.md](docs/SPECIFICATION_EN.md): task contracts and assessment reference.

The supplied Java entry point, JSON parser, input helpers and public checking tools
are kept unchanged. The other language folders are unselected template stubs.
The GitHub workflow uses Java 17 on Ubuntu and runs both public and own checks.

## Submission

Submit the exact commit URL in Moodle Online text and click **Submit assignment**.
Public checks report pass/fail; Moodle grades and the separate quiz are handled by the course.
The deadline is the one announced in Moodle.

## Source

The starter structure, adapters, public cases and checking tools come from the
[official assignment template](https://github.com/RZhaniya/daa-assignment-3-graphs/tree/76c1d91daa90ceadcb225f5408d54d4bfcf397f9).
