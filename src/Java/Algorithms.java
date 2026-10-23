import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/** Graph algorithms. Every call creates its own state. */
public final class Algorithms {
    private Algorithms() {
    }

    private static class DisjointSet {
        int[] parent;
        int[] size;
        int components;
        int largest;

        DisjointSet(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = n == 0 ? 0 : 1;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
            }
        }

        int find(int vertex) {
            int root = vertex;
            while (parent[root] != root) {
                root = parent[root];
            }
            // Compress the whole path without recursion.
            while (vertex != root) {
                int next = parent[vertex];
                parent[vertex] = root;
                vertex = next;
            }
            return root;
        }

        boolean union(int u, int v) {
            int a = find(u);
            int b = find(v);
            if (a == b) {
                return false;
            }
            if (size[a] < size[b]) {
                int temp = a;
                a = b;
                b = temp;
            }
            parent[b] = a;
            size[a] += size[b];
            components--;
            largest = Math.max(largest, size[a]);
            return true;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);
        DisjointSet sets = new DisjointSet(n);
        int[][] states = new int[operations.length][2];
        for (int i = 0; i < operations.length; i++) {
            sets.union(operations[i][0], operations[i][1]);
            states[i][0] = sets.components;
            states[i][1] = sets.largest;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("states", states);
        return result;
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<Integer> sortedIds = new ArrayList<>();
        for (int id = 0; id < edges.length; id++) {
            sortedIds.add(id);
        }
        sortedIds.sort((a, b) -> Integer.compare(edges[a][2], edges[b][2]));

        DisjointSet sets = new DisjointSet(n);
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        for (int id : sortedIds) {
            int[] edge = edges[id];
            if (sets.union(edge[0], edge[1])) {
                selected.add(id);
                cost += edge[2];
            }
        }
        return forestResult(cost, selected, sets.components);
    }

    private static class QueueEdge {
        int to;
        int weight;
        int id;

        QueueEdge(int to, int weight, int id) {
            this.to = to;
            this.weight = weight;
            this.id = id;
        }
    }

    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<List<Integer>> graph = emptyGraph(n);
        for (int id = 0; id < edges.length; id++) {
            graph.get(edges[id][0]).add(id);
            graph.get(edges[id][1]).add(id);
        }
        PriorityQueue<QueueEdge> queue = new PriorityQueue<>((a, b) -> {
            int comparison = Integer.compare(a.weight, b.weight);
            return comparison == 0 ? Integer.compare(a.id, b.id) : comparison;
        });
        boolean[] visited = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        long cost = 0;
        int components = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            queue.add(new QueueEdge(start, 0, -1));
            while (!queue.isEmpty()) {
                QueueEdge current = queue.remove();
                int vertex = current.to;
                if (visited[vertex]) {
                    continue;
                }
                visited[vertex] = true;
                if (current.id != -1) {
                    cost += current.weight;
                    selected.add(current.id);
                }
                for (int id : graph.get(vertex)) {
                    int[] edge = edges[id];
                    int next = edge[0] == vertex ? edge[1] : edge[0];
                    if (!visited[next]) {
                        queue.add(new QueueEdge(next, edge[2], id));
                    }
                }
            }
        }
        return forestResult(cost, selected, components);
    }

    private static Map<String, Object> forestResult(long cost, List<Integer> ids, int components) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cost", cost);
        result.put("edge_ids", ids);
        result.put("components", components);
        return result;
    }

    private static List<List<Integer>> emptyGraph(int n) {
        List<List<Integer>> graph = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        return graph;
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        List<List<Integer>> graph = emptyGraph(n);
        List<List<Integer>> reverse = emptyGraph(n);
        for (int[] edge : edges) {
            graph.get(edge[0]).add(edge[1]);
            reverse.get(edge[1]).add(edge[0]);
        }

        boolean[] visited = new boolean[n];
        int[] nextNeighbour = new int[n];
        int[] stack = new int[n];
        List<Integer> finishOrder = new ArrayList<>();
        // First DFS pass: save vertices only after all their neighbours finish.
        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }
            int stackSize = 0;
            stack[stackSize++] = start;
            visited[start] = true;
            while (stackSize > 0) {
                int vertex = stack[stackSize - 1];
                if (nextNeighbour[vertex] < graph.get(vertex).size()) {
                    int next = graph.get(vertex).get(nextNeighbour[vertex]++);
                    if (!visited[next]) {
                        visited[next] = true;
                        stack[stackSize++] = next;
                    }
                } else {
                    finishOrder.add(vertex);
                    stackSize--;
                }
            }
        }

        int[] component = new int[n];
        Arrays.fill(component, -1);
        int label = 0;
        // Second DFS pass on the transpose, in decreasing finish order.
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            int start = finishOrder.get(i);
            if (component[start] != -1) {
                continue;
            }
            int stackSize = 0;
            stack[stackSize++] = start;
            component[start] = label;
            while (stackSize > 0) {
                int vertex = stack[--stackSize];
                for (int next : reverse.get(vertex)) {
                    if (component[next] == -1) {
                        component[next] = label;
                        stack[stackSize++] = next;
                    }
                }
            }
            label++;
        }

        List<int[]> condensation = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int[] edge : edges) {
            int from = component[edge[0]];
            int to = component[edge[1]];
            if (from != to) {
                long pair = ((long) from << 32) | (to & 0xffffffffL);
                if (seen.add(pair)) {
                    condensation.add(new int[]{from, to});
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("component", component);
        result.put("condensation", condensation);
        return result;
    }

    private static List<List<Integer>> outgoingEdges(int n, int[][] edges) {
        List<List<Integer>> graph = emptyGraph(n);
        for (int id = 0; id < edges.length; id++) {
            graph.get(edges[id][0]).add(id);
        }
        return graph;
    }

    /** Returns null on a cycle; otherwise returns reverse DFS finish order. */
    private static int[] topologicalOrder(List<List<Integer>> graph, int[][] edges) {
        int n = graph.size();
        int[] state = new int[n]; // 0 = unvisited, 1 = active, 2 = finished
        int[] nextNeighbour = new int[n];
        int[] stack = new int[n];
        int[] order = new int[n];
        int position = n;
        for (int start = 0; start < n; start++) {
            if (state[start] != 0) {
                continue;
            }
            int stackSize = 0;
            stack[stackSize++] = start;
            state[start] = 1;
            while (stackSize > 0) {
                int vertex = stack[stackSize - 1];
                if (nextNeighbour[vertex] < graph.get(vertex).size()) {
                    int id = graph.get(vertex).get(nextNeighbour[vertex]++);
                    int next = edges[id][1];
                    if (state[next] == 1) {
                        return null;
                    }
                    if (state[next] == 0) {
                        state[next] = 1;
                        stack[stackSize++] = next;
                    }
                } else {
                    state[vertex] = 2;
                    order[--position] = vertex;
                    stackSize--;
                }
            }
        }
        return order;
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);
        int[] order = topologicalOrder(outgoingEdges(n, edges), edges);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acyclic", order != null);
        result.put("order", order == null ? new int[0] : order);
        return result;
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        List<List<Integer>> graph = outgoingEdges(n, edges);
        int[] order = topologicalOrder(graph, edges);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("acyclic", order != null);
        if (order == null) {
            result.put("distance", new Long[0]);
            result.put("parent_edge", new int[0]);
            return result;
        }

        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;
        for (int vertex : order) {
            if (distance[vertex] == null) {
                continue;
            }
            for (int id : graph.get(vertex)) {
                int next = edges[id][1];
                long candidate = distance[vertex] + edges[id][2];
                if (distance[next] == null || candidate < distance[next]) {
                    distance[next] = candidate;
                    parentEdge[next] = id;
                }
            }
        }
        result.put("distance", distance);
        result.put("parent_edge", parentEdge);
        return result;
    }

    /** Use with parentEdge from an acyclic DAG result. Empty list means no route. */
    public static List<Integer> reconstructRoute(int source, int target, int[][] edges, int[] parentEdge) {
        List<Integer> route = new ArrayList<>();
        if (target != source && parentEdge[target] == -1) {
            return route;
        }
        int current = target;
        route.add(current);
        while (current != source) {
            int id = parentEdge[current];
            current = edges[id][0];
            route.add(current);
        }
        Collections.reverse(route);
        return route;
    }
}
