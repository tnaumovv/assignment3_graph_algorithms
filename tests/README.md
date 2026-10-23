# Own tests

Run from the repository root with Java 17+ and Python 3.12+:

```sh
python3 tests/check.py --all
```

This builds the selected Java solution before running tests. No third-party Python
or Java packages are used. The supplied semantic validator checks witnesses against
independently established reference values.

## Hand-checked inputs

`cases.json` contains 33 named requests, expected answers and a reason for each answer.
`my_cases.jsonl` and `expected.jsonl` contain the same requests and reference answers
in JSON Lines form. Some valid outputs differ in edge order, SCC labels, topological
order or tied parents, so exact string comparison is not used for these tasks.

```sh
python3 tools/run.py --input tests/my_cases.jsonl
python3 tests/check.py --small
```

Actual answers are saved in `results/actual.jsonl`. The fixture batch is also run
three times in one Java process (99 requests) to check state reset and ping.
`results/witness-checks.json` records five alternative valid answers accepted and
13 invalid witnesses rejected: duplicate/missing forest edges, a selected self-loop,
wrong SCC labels or condensation, invalid topological orders and wrong parent edges.

## Independent small-graph checks

```sh
python3 tests/check.py --random
```

Seed `20261023` produces 80 graph rounds and 470 requests, then repeats the requests
forward and backward in one process (940 requests). Expected answers come from:

- Undirected flood fills after each union for DSU.
- Exhaustive subsets of edges for the optimal forest cost.
- Mutual reachability for SCC membership.
- Kahn's algorithm for global acyclicity.
- Bellman-Ford relaxation for shortest distances in acyclic graphs.

These references do not call the Java algorithms. Inputs, expected answers and actual
outputs are stored together in `results/random-cases.json.gz`.

## Full limits and deep traversal

```sh
python3 tests/check.py --scale
```

Nine generated requests each have 100000 vertices and 250000 edges/operations.
They cover repeated/self unions, positive 64-bit forest costs, negative loops,
parallel edges, a deep directed chain, a deep directed cycle, exact SCC condensation,
a 64-bit negative DAG chain and a cycle outside source reachability.

`scale_cases()` in `check.py` defines every input and explains its expected answer.
Every result is validated under `-Xmx384m` with the supplied 20-second process limit.
`results/scale-inputs.jsonl.gz` contains the complete input requests, and
`results/scale-actual.jsonl.gz` contains complete actual outputs in the same order.
`results/scale-summary.json` records the reasoning, SHA-256 hashes and elapsed process
times. These times include transport and are not used as the algorithm benchmark.

Read the compressed files with the standard library, for example:

```sh
python3 -c "import gzip; print(gzip.open('tests/results/scale-inputs.jsonl.gz', 'rt').readline()[:200])"
```

## Route helper

`RouteTests.java` directly calls `Algorithms.reconstructRoute`. The cases cover a
reachable target, source, unreachable target, tied paths, tied parallel edges,
a cheaper parallel edge and a 100000-vertex route. Each small route is checked for
direction, selected original edge IDs and cost. Actual results are in
`results/route-helper.json`.

## Benchmark

```sh
python3 tests/benchmark.py
```

`Benchmark.java` creates three seeded connected graphs, performs five warm-up calls
and five measured calls per method per graph, and verifies every returned forest
outside the timed section. It alternates which method runs first. The expected
optimum is independently known from the generated negative chain; all extra edges
are nonnegative. Inputs are reproducible from the Java generator and recorded seeds.
`results/benchmark.json` contains samples, medians, graph hashes and environment.

## Public tool checks

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/run.py --setup
python3 tools/run.py --check
```

The tool suite also tests unselected C++ and C# adapters. On the local macOS machine,
Apple Clang lacks the GNU-only `bits/stdc++.h` header and the .NET SDK is absent:
one C++ transport test fails and two C# tests are skipped. The unchanged full tool
suite also runs on Ubuntu in GitHub Actions with GNU C++ and .NET installed.
The selected Java solution is checked independently. Logs and runtime details are in
`results/tool-checks.txt`, `results/public-checks.txt`, `results/own-checks.txt` and
`results/verification.json`.
