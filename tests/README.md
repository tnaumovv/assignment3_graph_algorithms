# Tests

Run from the repository root with JDK 17+ and Python 3.12+:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/run.py --check
python3 tests/check.py --all
python3 tests/benchmark.py
```

## Inputs and expected answers

`cases.json` contains 33 requests with expected answers and explanations.
`my_cases.jsonl` contains the runnable input batch. Actual outputs are stored in
`results/actual.jsonl`; non-unique answers are validated semantically.
The fixture batch is also repeated three times in one process (99 requests).

`check.py --random` uses seed 20261023 and independent references: flood fills,
exhaustive forest subsets, mutual reachability, Kahn's algorithm and Bellman-Ford.
It checks 470 requests, then 940 repeated/reversed requests in one process.
Inputs, expected answers and actual outputs are in `results/random-cases.json.gz`.

`results/witness-checks.json` records five accepted alternative answers and
13 rejected invalid witnesses. `RouteTests.java` directly tests reachable,
source, unreachable, tied/parallel-edge and 100000-vertex routes; actual routes,
edge IDs and costs are in `results/route-helper.json`.

## Full limits

`check.py --scale` checks nine requests, each with 100000 vertices and 250000
edges/operations, using a 384 MiB heap and a 20-second process timeout.
`scale_cases()` defines the inputs and independently justified expected results.
Complete inputs and outputs are in `results/scale-inputs.jsonl.gz` and
`results/scale-actual.jsonl.gz`. `results/scale-summary.json` records their hashes,
expected-answer reasoning and actual process times.

## Benchmark

`benchmark.py` compiles and runs `Benchmark.java`. Three seeded graphs are used,
with five warm-up and five measured calls per method per size. Every returned
forest is checked outside the timed section against the known optimum.
`results/benchmark.json` contains raw samples, medians, seeds, hashes and environment.
`ANALYSIS.md` documents the generator, timing boundary, results and limitations.
