"""Own fixtures, independent small-graph oracles and full-limit checks."""
import argparse
import copy
import gzip
import hashlib
import itertools
import json
from pathlib import Path
import platform
import random
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import run
from public_validate import check

RESULTS = ROOT / 'tests' / 'results'
SEED = 20261023


def encoded(value):
    return json.dumps(value, separators=(',', ':'), ensure_ascii=False) + '\n'


def save(name, value):
    (RESULTS / name).write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def groups(n, edges):
    """Undirected flood fills, deliberately independent of the Java DSU."""
    graph = [[] for _ in range(n)]
    for edge in edges:
        u, v = edge[:2]
        graph[u].append(v)
        graph[v].append(u)
    seen = set()
    result = []
    for start in range(n):
        if start in seen:
            continue
        reached = {start}
        stack = [start]
        seen.add(start)
        while stack:
            for v in graph[stack.pop()]:
                if v not in seen:
                    seen.add(v)
                    reached.add(v)
                    stack.append(v)
        result.append(reached)
    return result


def dsu_oracle(request):
    edges, states = [], []
    for operation in request['operations']:
        edges.append(operation)
        parts = groups(request['n'], edges)
        states.append([len(parts), max(map(len, parts), default=0)])
    return {'states': states}


def forest_oracle(request):
    """Enumerate all candidate forests; no greedy algorithm is used."""
    n, edges = request['n'], request['edges']
    components = len(groups(n, edges))
    best_cost, best_ids = None, None
    for ids in itertools.combinations(range(len(edges)), n - components):
        chosen = [edges[i] for i in ids]
        if len(groups(n, chosen)) == components:
            cost = sum(edge[2] for edge in chosen)
            if best_cost is None or cost < best_cost:
                best_cost, best_ids = cost, list(ids)
    return {'cost': best_cost, 'edge_ids': best_ids, 'components': components}


def scc_oracle(request):
    """An SCC is determined by mutual reachability from every vertex."""
    n, edges = request['n'], request['edges']
    graph = [[] for _ in range(n)]
    for u, v in edges:
        graph[u].append(v)
    reachable = []
    for start in range(n):
        seen, stack = {start}, [start]
        while stack:
            for v in graph[stack.pop()]:
                if v not in seen:
                    seen.add(v)
                    stack.append(v)
        reachable.append(seen)
    labels = [-1] * n
    for u in range(n):
        if labels[u] == -1:
            for v in range(n):
                if v in reachable[u] and u in reachable[v]:
                    labels[v] = u
    pairs = sorted({(labels[u], labels[v]) for u, v in edges if labels[u] != labels[v]})
    return {'component': labels, 'condensation': [list(pair) for pair in pairs]}


def topo_oracle(request):
    """Kahn's algorithm checks the DFS implementation independently."""
    n, edges = request['n'], request['edges']
    graph, indegree = [[] for _ in range(n)], [0] * n
    for edge in edges:
        u, v = edge[:2]
        graph[u].append(v)
        indegree[v] += 1
    ready = [v for v in range(n) if indegree[v] == 0]
    order = []
    while ready:
        u = ready.pop()
        order.append(u)
        for v in graph[u]:
            indegree[v] -= 1
            if indegree[v] == 0:
                ready.append(v)
    acyclic = len(order) == n
    return {'acyclic': acyclic, 'order': order if acyclic else []}


def dag_oracle(request):
    if not topo_oracle(request)['acyclic']:
        return {'acyclic': False, 'distance': [], 'parent_edge': []}
    n, source, edges = request['n'], request['source'], request['edges']
    distance, parent = [None] * n, [-1] * n
    distance[source] = 0
    # Bellman-Ford relaxation, not topological-order relaxation.
    for _ in range(n - 1):
        changed = False
        for i, (u, v, w) in enumerate(edges):
            if distance[u] is not None and (distance[v] is None or distance[u] + w < distance[v]):
                distance[v], parent[v], changed = distance[u] + w, i, True
        if not changed:
            break
    return {'acyclic': True, 'distance': distance, 'parent_edge': parent}


def validate_batch(command, cases):
    answers = run.execute(command, [case['input'] for case in cases])
    for case, answer in zip(cases, answers):
        if not check(case['input'], answer, case['output']):
            raise AssertionError(f"{case['name']}: expected {case['output']}, received {answer}")
    return answers


def small_checks(command):
    cases = json.loads((ROOT / 'tests' / 'cases.json').read_text())
    answers = validate_batch(command, cases)
    (RESULTS / 'actual.jsonl').write_text(''.join(map(encoded, answers)), encoding='utf-8')
    # Three complete batches in one process include ping and all six tasks.
    validate_batch(command, cases * 3)
    save('small-summary.json', {'fixtures': len(cases), 'mixed_requests': len(cases) * 3,
                                'passed': True})
    print(f'PASS own fixtures: {len(cases)}; mixed state-reset requests: {len(cases) * 3}')
    return cases


def witness_checks(cases):
    by_name = {case['name']: case for case in cases}
    records = []

    def trial(name, case, answer, should_accept):
        accepted = check(case['input'], answer, case['output'])
        records.append({'name': name, 'request': case['input'], 'expected': case['output'],
                        'candidate': answer, 'should_accept': should_accept, 'accepted': accepted})
        assert accepted == should_accept, name

    for task in ('kruskal', 'prim'):
        case = by_name[task + '-ties']
        trial(task + '-alternative-tree', case, dict(case['output'], edge_ids=[0, 2]), True)
        trial(task + '-duplicate-edge', case, dict(case['output'], edge_ids=[0, 0]), False)
        trial(task + '-missing-edge', case, dict(case['output'], edge_ids=[0]), False)
        case = by_name[task + '-negative-parallel-disconnected']
        trial(task + '-selected-loop', case, dict(case['output'], edge_ids=[0, 2, 5]), False)
    case = by_name['scc-dedup']
    alternative = {'component': [7, 7, 12, 12, 20, 99], 'condensation': [[7, 12], [12, 20]]}
    trial('scc-alternative-labels', case, alternative, True)
    duplicate = copy.deepcopy(alternative)
    duplicate['condensation'].append([7, 12])
    trial('scc-duplicate-condensation', case, duplicate, False)
    trial('scc-missing-condensation', case, dict(alternative, condensation=[]), False)
    trial('scc-merged-isolate', case, dict(alternative, component=[7, 7, 12, 12, 20, 20]), False)
    case = by_name['topo-isolates']
    trial('topo-alternative-order', case, {'acyclic': True, 'order': [3, 2, 1, 0]}, True)
    trial('topo-duplicate-vertex', case, {'acyclic': True, 'order': [0, 1, 2, 2]}, False)
    case = by_name['topo-finished-neighbour']
    trial('topo-backward-dependency', case, {'acyclic': True, 'order': [0, 1, 2, 3]}, False)
    case = by_name['dag-ties-parallel']
    trial('dag-alternative-parents', case, dict(case['output'], parent_edge=[-1, 1, 2, 4, -1]), True)
    trial('dag-edge-ending-at-wrong-vertex', case, dict(case['output'], parent_edge=[-1, 0, 2, 0, -1]), False)
    trial('dag-parent-on-unreachable-vertex', case, dict(case['output'], parent_edge=[-1, 0, 2, 3, 4]), False)
    save('witness-checks.json', records)
    print(f'PASS semantic checker: {sum(r["should_accept"] for r in records)} alternative answers accepted; '
          f'{sum(not r["should_accept"] for r in records)} invalid witnesses rejected')


def random_checks(command):
    rng, cases = random.Random(SEED), []
    for number in range(80):
        n = rng.randrange(8)
        weighted = [[rng.randrange(n), rng.randrange(n), rng.randint(-8, 8)]
                    for _ in range(rng.randrange(11))] if n else []
        operations = [[rng.randrange(n), rng.randrange(n)] for _ in range(10)] if n else []
        request = {'task': 'dsu', 'n': n, 'operations': operations}
        cases.append({'name': f'random-{number}-dsu', 'input': request, 'output': dsu_oracle(request)})
        for task in ('kruskal', 'prim'):
            request = {'task': task, 'n': n, 'edges': weighted}
            cases.append({'name': f'random-{number}-{task}', 'input': request, 'output': forest_oracle(request)})
        directed = [edge[:2] for edge in weighted]
        for task, oracle in [('scc', scc_oracle), ('topo', topo_oracle)]:
            request = {'task': task, 'n': n, 'edges': directed}
            cases.append({'name': f'random-{number}-{task}', 'input': request, 'output': oracle(request)})
        if n:
            # Half of the DAG requests are guaranteed acyclic; the rest can contain arbitrary cycles.
            dag_edges = [edge for edge in weighted if edge[0] < edge[1]] if number % 2 == 0 else weighted
            request = {'task': 'dag', 'n': n, 'source': rng.randrange(n), 'edges': dag_edges}
            cases.append({'name': f'random-{number}-dag', 'input': request, 'output': dag_oracle(request)})
    answers = validate_batch(command, cases)
    validate_batch(command, cases + list(reversed(cases)))
    with gzip.open(RESULTS / 'random-cases.json.gz', 'wt', encoding='utf-8') as stream:
        json.dump([dict(case, actual=answer) for case, answer in zip(cases, answers)], stream)
    save('random-summary.json', {'seed': SEED, 'generated_graphs': 80, 'requests': len(cases),
                                 'mixed_requests': len(cases) * 2, 'passed': True,
                                 'oracles': ['flood fill', 'exhaustive forest subsets',
                                             'mutual reachability', 'Kahn', 'Bellman-Ford']})
    print(f'PASS independent oracles: {len(cases)} requests; repeated/reversed batch: {len(cases) * 2}')


def scale_cases():
    n, limit = 100000, 250000
    operations = [[i, i + 1] for i in range(n - 1)]
    operations += [[i % n, i % n] if i % 2 == 0 else [0, i % n]
                   for i in range(limit - len(operations))]
    states = [[n - i - 1, i + 2] for i in range(n - 1)] + [[1, n]] * (limit - n + 1)
    yield 'dsu-full-limit', {'task': 'dsu', 'n': n, 'operations': operations}, {'states': states}, \
          'The first n-1 operations make a chain; remaining unions change nothing.'
    del operations, states

    edges = [[i, i + 1, 1000000000] for i in range(n - 1)]
    edges += [[i % n, i % n, -1000000000] if i % 7 == 0 else [0, i % n, 1000000000]
              for i in range(limit - len(edges))]
    for task in ('kruskal', 'prim'):
        yield task + '-full-limit', {'task': task, 'n': n, 'edges': edges}, \
              {'cost': (n - 1) * 1000000000, 'components': 1}, \
              'Every usable edge weighs 1e9; any spanning tree needs n-1 edges. Negative loops are forbidden.'
    del edges

    edges = [[i, i + 1] for i in range(n - 1)]
    edges += [[0, 1 + i % (n - 1)] for i in range(limit - len(edges))]
    yield 'scc-deep-dag', {'task': 'scc', 'n': n, 'edges': edges}, {'component': list(range(n))}, \
          'All edges go forward, so every vertex is a separate SCC; condensation follows input cross pairs.'
    yield 'topo-deep-dag', {'task': 'topo', 'n': n, 'edges': edges}, {'acyclic': True}, \
          'The chain forces the unique order 0 through n-1; extra edges go forward.'
    cyclic = edges[:-1] + [[n - 1, 0]]
    yield 'topo-deep-cycle', {'task': 'topo', 'n': n, 'edges': cyclic}, {'acyclic': False, 'order': []}, \
          'The final edge closes the 100000-vertex chain into a cycle.'
    del edges, cyclic

    ring = [[i, (i + 1) % n] for i in range(n)]
    ring += [[i % n, (i + 1) % n] for i in range(limit - len(ring))]
    yield 'scc-deep-cycle', {'task': 'scc', 'n': n, 'edges': ring}, \
          {'component': [0] * n, 'condensation': []}, \
          'The directed ring makes every vertex reachable from every other; duplicates add no SCCs.'
    del ring

    weighted = [[i, i + 1, -1000000000] for i in range(n - 1)]
    weighted += [[0, 1 + i % (n - 1), 0] for i in range(limit - len(weighted))]
    yield 'dag-deep-negative', {'task': 'dag', 'n': n, 'source': 0, 'edges': weighted}, \
          {'acyclic': True, 'distance': [-i * 1000000000 for i in range(n)]}, \
          'The full negative chain is shorter than every zero-cost shortcut; distance[i] = -i*1e9.'
    del weighted

    hidden = [[i, (i + 1) % (n - 1), 1] for i in range(n - 1)]
    hidden += [[0, 1, 1] for _ in range(limit - len(hidden))]
    yield 'dag-full-unreachable-cycle', {'task': 'dag', 'n': n, 'source': n - 1, 'edges': hidden}, \
          {'acyclic': False, 'distance': [], 'parent_edge': []}, \
          'The source is isolated, but the other 99999 vertices contain a cycle.'


def scale_checks(command):
    summary = []
    with gzip.open(RESULTS / 'scale-inputs.jsonl.gz', 'wt', encoding='utf-8') as inputs, \
            gzip.open(RESULTS / 'scale-actual.jsonl.gz', 'wt', encoding='utf-8') as outputs:
        for name, request, expected, reason in scale_cases():
            request_text = encoded(request)
            inputs.write(request_text)
            started = time.perf_counter()
            answer = run.execute(command, [request])[0]
            elapsed = time.perf_counter() - started
            assert check(request, answer, expected), name
            answer_text = encoded(answer)
            outputs.write(answer_text)
            summary.append({'name': name, 'n': request['n'],
                            'edges_or_operations': len(request.get('edges', request.get('operations', []))),
                            'expected_reason': reason, 'passed': True,
                            'request_sha256': hashlib.sha256(request_text.encode()).hexdigest(),
                            'answer_sha256': hashlib.sha256(answer_text.encode()).hexdigest(),
                            'process_seconds_including_transport': round(elapsed, 6)})
            print(f'PASS scale: {name}, n={request["n"]}, m/q=250000 ({elapsed:.3f}s)', flush=True)
    save('scale-summary.json', {'environment': platform.platform(), 'heap': '-Xmx384m',
                               'timeout_per_request_seconds': 20, 'cases': summary})


def route_checks(command):
    output = ROOT / '.build' / 'Java'
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-cp', str(output),
                    '-d', str(output), str(ROOT / 'tests' / 'RouteTests.java')], check=True, timeout=60)
    completed = subprocess.run(command[:-1] + ['RouteTests'], capture_output=True, text=True,
                               encoding='utf-8', check=True, timeout=20)
    records = [json.loads(line) for line in completed.stdout.splitlines()]
    save('route-helper.json', records)
    print(f'PASS route helper: {len(records)} cases, including a 100000-vertex route')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    for flag in ('small', 'random', 'scale', 'all'):
        modes.add_argument('--' + flag, action='store_true')
    args = parser.parse_args()
    RESULTS.mkdir(parents=True, exist_ok=True)
    command = run.build('Java')
    if args.all or args.small or not (args.random or args.scale):
        witness_checks(small_checks(command))
        route_checks(command)
    if args.all or args.random:
        random_checks(command)
    if args.all or args.scale:
        scale_checks(command)


if __name__ == '__main__':
    main()
