import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** A small reproducible benchmark; validation is outside the timed section. */
public class Benchmark {
    private static final long SEED = 20261023L;
    private static final int WARMUP = 5;
    private static final int REPEATS = 5;

    private static Map<String, Object> solve(String method, Map<String, Object> input) {
        return method.equals("kruskal") ? Algorithms.kruskal(input) : Algorithms.prim(input);
    }

    private static void verify(Map<String, Object> answer, int n, int[][] edges, long expectedCost) {
        List<?> ids = (List<?>) answer.get("edge_ids");
        if (((Number) answer.get("cost")).longValue() != expectedCost
                || ((Number) answer.get("components")).intValue() != 1 || ids.size() != n - 1) {
            throw new AssertionError("Incorrect forest cost, size or component count");
        }
        boolean[] used = new boolean[edges.length];
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        long sum = 0;
        for (Object value : ids) {
            int id = ((Number) value).intValue();
            if (id < 0 || id >= edges.length || used[id] || edges[id][0] == edges[id][1]) {
                throw new AssertionError("Invalid selected edge ID");
            }
            used[id] = true;
            int[] edge = edges[id];
            graph.get(edge[0]).add(edge[1]);
            graph.get(edge[1]).add(edge[0]);
            sum += edge[2];
        }
        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        int size = 0;
        int count = 0;
        stack[size++] = 0;
        seen[0] = true;
        while (size > 0) {
            int vertex = stack[--size];
            count++;
            for (int next : graph.get(vertex)) {
                if (!seen[next]) {
                    seen[next] = true;
                    stack[size++] = next;
                }
            }
        }
        // A connected graph with n-1 distinct edges is a tree.
        if (count != n || sum != expectedCost) {
            throw new AssertionError("Selected edges are not the expected spanning tree");
        }
    }

    private static double measure(String method, Map<String, Object> input,
                                  int n, int[][] edges, long expectedCost) {
        long start = System.nanoTime();
        Map<String, Object> answer = solve(method, input);
        long elapsed = System.nanoTime() - start;
        verify(answer, n, edges, expectedCost);
        return elapsed / 1000000.0;
    }

    private static double median(List<Double> samples) {
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static void benchmark(int n, int m) throws Exception {
        Random random = new Random(SEED + n);
        List<List<Integer>> rows = new ArrayList<>();
        long expectedCost = 0;
        // These negative chain edges form the known optimal tree.
        for (int i = 0; i < n - 1; i++) {
            int weight = -1 - random.nextInt(1000000);
            rows.add(List.of(i, i + 1, weight));
            expectedCost += weight;
        }
        // All additional edges are nonnegative and cannot improve that tree.
        while (rows.size() < m) {
            int u = random.nextInt(n);
            int v = random.nextInt(n);
            if (u != v) {
                rows.add(List.of(u, v, random.nextInt(1000001)));
            }
        }
        Collections.shuffle(rows, random);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("n", n);
        input.put("edges", rows);
        int[][] edges = Input.edges(input);
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(Json.write(input).getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte value : digest) {
            hash.append(String.format("%02x", value & 255));
        }

        for (String method : List.of("kruskal", "prim")) {
            verify(solve(method, input), n, edges, expectedCost);
        }
        for (int i = 0; i < WARMUP; i++) {
            verify(solve("kruskal", input), n, edges, expectedCost);
            verify(solve("prim", input), n, edges, expectedCost);
        }
        List<Double> kruskal = new ArrayList<>();
        List<Double> prim = new ArrayList<>();
        for (int i = 0; i < REPEATS; i++) {
            if (i % 2 == 0) {
                kruskal.add(measure("kruskal", input, n, edges, expectedCost));
                prim.add(measure("prim", input, n, edges, expectedCost));
            } else {
                prim.add(measure("prim", input, n, edges, expectedCost));
                kruskal.add(measure("kruskal", input, n, edges, expectedCost));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("n", n);
        result.put("m", m);
        result.put("seed", SEED + n);
        result.put("graph_sha256", hash.toString());
        result.put("expected_cost", expectedCost);
        result.put("warmup_per_method", WARMUP);
        result.put("measurements_per_method", REPEATS);
        result.put("verified_outputs_per_method", 1 + WARMUP + REPEATS);
        result.put("kruskal_ms", kruskal);
        result.put("prim_ms", prim);
        result.put("kruskal_median_ms", median(kruskal));
        result.put("prim_median_ms", median(prim));
        result.put("all_forests_verified", true);
        System.out.println(Json.write(result));
    }

    public static void main(String[] args) throws Exception {
        benchmark(1000, 4000);
        benchmark(10000, 40000);
        benchmark(100000, 250000);
    }
}
