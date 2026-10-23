# Assignment 3: Analysis

## 1. Program and data

The solution uses Java 17 and the supplied UTF-8 JSON Lines interface.
The six methods are in `src/Java/Algorithms.java`. The provided `Main.java`,
`Input.java`, `Json.java` and public checking tools are unchanged.
`submission.json` selects Java. Standard lists, sorting, a priority queue and a
hash set are used; the graph algorithms are implemented in the solution.

Let `n` be the number of vertices, `m` the number of edges, `q` the number of union
operations and `c` the number of connected components. Vertices are numbered
`0..n-1`. An edge ID is its original position in the input array. The code sorts
IDs rather than the input edges, so sorting never changes their meaning.

Each request creates new arrays, lists, sets and queues. No mutable algorithm
state is stored between requests. The entry point keeps ping and prints exactly
one result object per input line. Debug text is not written to program stdout.

Weights fit `int`, but costs and distances use `long`. Any simple path or forest
has at most `n-1` edges. At the published limits its absolute weight is at most
`99999 * 1000000000 = 99999000000000`, which fits signed 64-bit arithmetic.
`Long[]` stores nullable distances. Unreachable is `null`, not `-1`, because a
finite shortest distance can equal `-1`.

## 2. DSU

### Steps

Initially each vertex is its own root, has size 1, and the number of components
is `n`. The largest size is 1 for a nonempty graph and 0 for an empty graph.
`find` first walks to the root, then walks the same path again and points each
vertex directly to the root. This is iterative path compression.

For a union, find both roots. If they are equal, leave the partition unchanged.
Otherwise attach the smaller set below the larger set, add their sizes, decrease
the component count and update the largest size. After every operation, append
`[components, largest]`; there is no extra initial state.

### Invariant and correctness

Every parent chain ends at the root of exactly one set. Only roots store the
current set sizes. Path compression changes parent links inside a set but does
not change its members. A successful union joins exactly two sets, so decreasing
the count by one and adding their sizes is correct. An unsuccessful union changes
neither the partition nor its statistics. By induction over operations, every
returned state describes the current graph connectivity.

### Boundaries and complexity

Repeated unions and self-unions change nothing. For `n=0`, there are no operations
and `states=[]`. For a singleton, self-unions return `[1,1]`.
Time is `O(n + q * alpha(n))` amortised with union by size and path compression.
Memory is `O(n+q)`, including the result states. `find` does not use recursion.

## 3. Kruskal

### Steps

Make a list of original edge IDs and sort that list by edge weight. Start a fresh
DSU. Scan the sorted IDs and select an edge only if its endpoints belong to
different DSU sets. Add its weight to a `long` total and keep its original ID.
Return the total, selected IDs and the final DSU component count.

### Invariant and correctness

The selected edges are always a forest because an edge is added only between
different current components. There is an optimal spanning forest containing
all selected edges. For the next accepted edge, its endpoints lie in different
selected components, and no lighter edge crossing that cut remains unprocessed.
The cut property therefore permits adding this edge without losing optimality.
After all edges are processed, every original connected component is spanned.
The result is a minimum spanning forest with exactly `n-c` edges.

### Boundaries and complexity

A self-loop is rejected because its roots are equal. Parallel and equal-weight
edges are considered independently, but only edges preserving the forest are
selected. Negative weights work with the same ordering. Isolates stay separate.
An empty graph returns cost 0, no IDs and 0 components.
Time is `O(n + m log(m+1))`; memory is `O(n+m)`, including IDs, input rows, DSU and
returned forest. Weight comparison uses `Integer.compare`, not subtraction.

## 4. Prim

### Steps

Store the original IDs incident to each vertex in an undirected adjacency list.
For each unvisited start vertex, increase the component count and put a root
entry into a min-priority queue. Take the lightest entry. Skip it if its target
has already been visited. Otherwise visit the target and select its incoming
edge unless the entry is the root marker. Add candidate edges to unvisited
neighbours. Finish the queue before starting the next component.

This is a separate algorithm from Kruskal: it does not sort all edges or use
DSU to select them. The root marker has ID -1 and contributes no cost.

### Invariant and correctness

Within one component, the selected edges form a tree on the visited vertices.
The queue contains every edge crossing from a visited vertex to an unvisited
vertex, possibly together with stale entries whose targets are already visited.
After stale entries are skipped, the minimum remaining entry is a minimum edge
crossing the visited/unvisited cut. The cut property makes its selection safe.
When the queue is empty, the whole connected component has been visited.
Restarting covers every component and isolate, producing an optimal forest.

### Boundaries and complexity

Loops lead back to a visited vertex and are never selected. Parallel edges,
negative weights and equal weights are supported. Different optimal edge ID
sets are allowed. An isolated start still counts as one component.
An empty graph has zero cost and components.
With the lazy heap, time is `O(n + m log(m+1))` and memory is `O(n+m)`.
Each undirected edge can create a candidate when its first endpoint is visited;
stale candidates are discarded later. The queue is empty before each restart.

## 5. Strongly connected components: Kosaraju

### Steps

Build the directed graph and its transpose. The first iterative DFS visits every
component and records each vertex after all its neighbours have been processed.
Then read that finish list backward. From each still-unlabelled vertex, run DFS
on the transpose and assign the same new nonnegative label to all reached vertices.

Finally scan every original edge. If its endpoint labels differ, add that pair
to the condensation once. A `HashSet<Long>` removes duplicates; the key places
the first label in the high 32 bits and the second in the low 32 bits. Since all
labels are nonnegative and below `n`, different label pairs have different keys.

### Invariant and correctness

In the first pass, a vertex enters the finish list only after its DFS descendants
finish. For any edge from SCC A to a different SCC B in the condensation graph,
the maximum first-pass finish time of A is greater than that of B. Consequently,
the next unprocessed SCC in decreasing finish order cannot reach a different
unprocessed SCC in the transpose. It can reach all of its own vertices because
strong connectivity is preserved by transposition. Thus each second-pass DFS
labels exactly one SCC. The final scan includes every cross-component pair and
excludes both internal edges and repeated pairs, giving the exact condensation.

### Boundaries and complexity

Each isolate becomes its own SCC. Loops remain inside an SCC. Repeated edges do
not create duplicate condensation pairs. For `n=0`, both arrays are empty.
Expected time is `O(n+m)` with hash-based deduplication; memory is `O(n+m)`,
including both graphs, DFS arrays, labels and condensation.

## 6. DFS topological ordering

### Steps

Use three states: 0 unvisited, 1 active and 2 finished. Start DFS from every
unvisited vertex. The explicit stack holds the current DFS path, and
`nextNeighbour[v]` remembers which outgoing edge should be processed next.
An edge to an active vertex returns a cycle immediately. An edge to a finished
vertex is harmless. When a vertex finishes, place it into the result array from
right to left, producing reverse finish order.

### Invariant and correctness

The active vertices are exactly the unfinished DFS path. An edge to an active
vertex closes a directed path into a cycle. Conversely, a directed cycle has a
back edge during DFS and is detected. In a graph without a cycle, every edge
`u -> v` has `v` finished before `u`: either DFS visits `v` from `u`, or `v` has
already finished. Reversing finish order therefore places `u` before `v`.
The outer loop includes every vertex, so the returned order is a full permutation.

### Boundaries and complexity

A cycle in any component causes `acyclic=false` and `order=[]`. A self-loop is a
cycle. Parallel edges and edges into finished vertices do not cause false cycles.
An empty graph returns `acyclic=true` and `order=[]`.
Time and memory are `O(n+m)`. The neighbour cursor advances once per edge;
returning to a suspended vertex never rescans all its earlier neighbours.

## 7. DAG shortest paths

### Steps

First run the whole-graph topological DFS, including components unreachable from
the source. If it finds any cycle, return `acyclic=false` and two empty arrays.
Otherwise initialise every distance to `null`, every parent to -1, and the source
distance to 0. Process vertices in topological order. Skip unreachable vertices.
For each outgoing edge from a reachable vertex, relax its target when a strictly
smaller distance is found, and store that original edge ID as the target's parent.
An equal-cost alternative leaves the current valid parent unchanged.

### Invariant and correctness

Before processing a vertex, all its predecessors have already been processed.
Every source path to that vertex ends at one of those predecessors, so relaxation
has considered every possible last edge. Its stored distance is therefore the
shortest distance, by induction along the topological order. Negative weights
are safe because there is no directed cycle and no predecessor is processed later.
A stored parent edge ends at its vertex and satisfies the distance equation.
Following parents moves backward in topological order and must reach the source.

### Boundaries and complexity

An unreachable cycle invalidates the entire result. Unreachable vertices retain
`distance=null` and `parent_edge=-1`; the source keeps distance 0 and parent -1.
A singleton with no edges is valid. Loops are cycles. Parallel edges and tied
shortest paths can produce different valid parents. Positive and negative totals
beyond 32 bits use `long`. Unlike the other tasks, valid DAG inputs require `n>=1`.
Time and memory are `O(n+m)`, including the returned arrays.

## 8. Route reconstruction

The helper is called directly; it is not a new JSON task:

```java
Algorithms.reconstructRoute(source, target, edges, parentEdge)
```

Its precondition is a valid acyclic DAG result with the original `edges` array.
For an unreachable target it returns an empty list. The route to the source is
`[source]`. For a reachable target, it follows incoming parent edge IDs backward,
collects the vertices and reverses the list. The same parent argument used above
proves that the route terminates at the source and uses the selected directed edges.
For a route of `L` vertices, time and output space are `O(L)`; no recursive calls
or full-graph scan are needed. Source and unreachable queries take `O(1)` time.

These direct calls ran in `tests/RouteTests.java`; complete outputs are in
`tests/results/route-helper.json`.

For `edgesA = [[0,1,3],[0,2,8],[1,2,-7],[2,3,4],[1,3,10],[4,5,-9]]`,
the actual parents were `[-1,0,2,3,-1,-1]`:

| Call | Actual vertex route | Selected edge IDs | Checked cost |
|---|---|---|---|
| `reconstructRoute(0, 3, edgesA, parentsA)` | `[0,1,2,3]` | `[0,2,3]` | `3-7+4 = 0` |
| `reconstructRoute(0, 0, edgesA, parentsA)` | `[0]` | `[]` | `0` |
| `reconstructRoute(0, 4, edgesA, parentsA)` | `[]` | `[]` | no route; distance is `null` |

For `edgesB = [[0,1,2],[0,1,2],[0,2,1],[1,3,-3],[2,3,-2]]`,
the actual parents were `[-1,0,2,4,-1]`:

| Call | Actual vertex route | Selected edge IDs | Checked cost |
|---|---|---|---|
| `reconstructRoute(0, 3, edgesB, parentsB)` | `[0,2,3]` | `[2,4]` | `1-2 = -1` |
| `reconstructRoute(0, 1, edgesB, parentsB)` | `[0,1]` | `[0]` | `2` |

The alternative parent array `[-1,1,2,3,-1]` is also valid: it uses the other
parallel edge into vertex 1 and the equally short route `0 -> 1 -> 3`.
The semantic checker explicitly accepted an alternative parent witness.
For `edgesC = [[0,1,4],[0,1,-2],[1,2,5]]`, parents were `[-1,1,2]`.
`reconstructRoute(0, 2, edgesC, parentsC)` returned `[0,1,2]`, with edge IDs
`[1,2]` and cost `-2+5=3`, showing why an edge ID matters for parallel edges.

A separate helper test reconstructed every vertex in a 100000-vertex chain.
Each test checks edge direction and total weight, not just the route length.

## 9. State reset, deep graphs and verification

All algorithm objects are local to one call. The graph, visited states, queue,
DSU and parent arrays are never reused for another request. Two full public
batches run in the same process. The own fixture suite repeats all tasks and ping
three times in one process, and the random suite repeats requests forward and
backward, detecting state that leaks across tasks or graph sizes.

The two Kosaraju passes, topological DFS, DSU find and route helper are iterative.
First-pass Kosaraju and topological DFS remember the next neighbour of every
suspended vertex. Vertices are marked when pushed, so an explicit stack never
needs more than `n` entries. Deep chains therefore do not depend on the JVM call
stack and still take linear time.

Actual checks on 2026-10-23 used Temurin 17.0.21+9, Java 17 bytecode, Python 3.13.1
and a 384 MiB Java heap. The artifacts contain actual outputs, not planned results.

| Check | Actual result |
|---|---|
| Supplied ping/setup | passed |
| Public cases | 36 passed |
| Public state reset | two complete batches passed in one process |
| Own justified fixtures | 33 passed |
| Own mixed fixture batch | 99 requests passed in one process |
| Alternative valid witnesses | 5 accepted |
| Invalid witnesses | 13 rejected |
| Direct route-helper tests | 7 passed, including a 100000-vertex route |
| Independent small-graph references | 470 requests passed, seed 20261023 |
| Mixed random batch | 940 requests passed in one process |
| Full-limit inputs | 9 passed, each with n=100000 and m/q=250000 |

The small references use flood fills for DSU, exhaustive edge subsets for forest
costs, mutual reachability for SCCs, Kahn's algorithm for acyclicity and Bellman-Ford
for distances. They do not use the solution to generate expected values.
`tests/cases.json` also gives hand-checked reasons for each fixed expected answer.

Non-unique answers are checked by forest feasibility and optimum cost, SCC
membership and exact condensation, topological precedence, and shortest distances
with valid parent paths. The suite accepts different valid IDs, labels, orders and
parents. It rejects duplicated or missing edges, loops in a forest, merged SCCs,
inexact condensation, repeated or misplaced topological vertices and invalid
parent-edge witnesses.

Full-limit expected values have simple independent reasons. A chain followed by
repeated unions eventually gives `[1,100000]`. A connected graph whose usable
edges all weigh 1e9 needs 99999 selected edges and has cost 99999000000000;
negative loops cannot be selected. A directed forward chain has separate SCCs
and a forced topological order, while a directed ring is one SCC and is cyclic.
For the negative DAG chain, `distance[i] = -i*1000000000`; zero-cost forward
shortcuts cannot improve those distances. An isolated source does not hide a
cycle in another component.

Complete compressed scale inputs and outputs are stored under `tests/results/`.
The summary records SHA-256 hashes, expected-answer reasoning, heap size and
process times; every scale request passed the supplied 20-second process limit.
These end-to-end process times are separate from the benchmark below.

The unmodified public tool unit suite was also run locally. Its Java/Python
checks passed. One unselected C++ transport test failed because macOS Apple Clang
lacks GNU `bits/stdc++.h`; two C# checks were skipped because .NET was not installed.
The full tool suite is additionally run by the Ubuntu GitHub workflow with GNU
C++ and .NET. The local limitation is preserved in `tests/results/tool-checks.txt`
and `tests/results/verification.json`; it is not reported as a passing local run.

## 10. Build, run and test commands

Run from the repository root with JDK 17+ and Python 3.12+ on PATH:

```sh
python3 tools/run.py --setup
python3 tools/run.py --check
python3 tools/run.py --input tests/my_cases.jsonl
python3 tests/check.py --small
python3 tests/check.py --random
python3 tests/check.py --scale
python3 tests/check.py --all
python3 tests/benchmark.py
python3 -m unittest discover -s tools -p 'test_*.py'
```

`--all` combines the small, helper, random and scale checks. The benchmark is run
separately. Python scripts use only the standard library. `python` or `py -3` can
replace `python3` when appropriate.

Build and run directly:

```sh
mkdir -p .build/Java
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java
java -Xmx384m -cp .build/Java Main < tests/my_cases.jsonl
```

## 11. Kruskal and Prim benchmark

The complete measurements are in `tests/results/benchmark.json`. Both methods
receive the same graph at each size, in one JVM. `tests/Benchmark.java` is the
input generator and measurement code; `tests/benchmark.py` compiles it first.

The generator uses `java.util.Random(20261023 + n)`. It creates a chain with
weights from -1000000 to -1, adds random nonnegative edges until reaching `m`,
and shuffles all rows with the same random object. All chain edges are cheaper
than any additional edge. They form a spanning tree, so their sum is the known
optimum before either method runs. The benchmark verifies the reported cost,
component count, original edge IDs, uniqueness, edge sum and connectivity.
A connected selected graph with `n-1` distinct edges is a tree. Validation runs
before warm-up and after every warm-up and measured call, outside timing.

There are five warm-up calls per method per graph after one initial verified call,
then five timed calls per method per graph. Which method runs first alternates
between rounds. The timer is `System.nanoTime()`; nanoseconds are converted to
milliseconds. Both boundaries are the full method invocation, including
`Input.edges` row copying, algorithm-specific adjacency construction, sorting or
heap operations, allocation and result creation. Compilation, JVM launch, input
generation, hashing, JSON parsing/output and witness verification are excluded.
Automatic garbage collection can still affect a measured call.

**Recorded environment:**

- CPU: Apple M2; architecture arm64.
- OS: macOS-15.7.1-arm64-arm-64bit-Mach-O.
- Runtime: openjdk version "17.0.21" 2026-10-20; OpenJDK Runtime Environment Temurin-17.0.21+9 (build 17.0.21+9); OpenJDK 64-Bit Server VM Temurin-17.0.21+9 (build 17.0.21+9, mixed mode, sharing).
- Heap: `-Xmx384m`; Python 3.13.1.
- Command: `python3 tests/benchmark.py`.

| n | m | Generator seed | Kruskal samples (ms) | Kruskal median (ms) | Prim samples (ms) | Prim median (ms) |
|---:|---:|---:|---|---:|---|---:|
| 1000 | 4000 | 20262023 | 2.462750, 3.847959, 2.218458, 1.780041, 1.775542 | 2.218458 | 2.007708, 1.790917, 1.751292, 1.573125, 1.565209 | 1.751292 |
| 10000 | 40000 | 20271023 | 18.681208, 15.590208, 15.664208, 15.504791, 15.586834 | 15.590208 | 16.374750, 18.431083, 18.022041, 17.856250, 18.798709 | 18.022041 |
| 100000 | 250000 | 20361023 | 115.003583, 120.756542, 134.151667, 126.016458, 139.824708 | 126.016458 | 146.388459, 155.392792, 163.966750, 143.400791, 147.775000 | 147.775000 |

All returned forests passed validation. The recorded graph SHA-256 hashes in the
JSON report allow the generated inputs to be checked on another run.

### Observations and limitations

Prim had the smaller median at the smallest size. Kruskal had the smaller median
at the medium and largest sizes. Sorting IDs and lazy-heap operations have
different constants and allocation patterns, even though both implemented methods
have the same stated asymptotic upper bound. The five samples show variation from
JIT compilation, garbage collection and operating-system scheduling.

These are measurements for one connected, sparse graph family on one computer.
The negative chain makes the optimum independently verifiable, but it also gives
the inputs a particular structure. Five samples and three sizes do not establish
that either method is always faster. The measured observations are separate from
the complexity proofs in Sections 3 and 4.

## 12. Sources

The task contracts, starter structure, JSON adapters, public cases and public
checking tools come from the official assignment repository:

- [Assignment template, commit 76c1d91](https://github.com/RZhaniya/daa-assignment-3-graphs/tree/76c1d91daa90ceadcb225f5408d54d4bfcf397f9).

The supplied support files are unchanged. The own fixture justifications,
independent references, direct helper tests and recorded measurements are included
with the solution so that the results can be reproduced.
