import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Direct calls to the route helper; no external test library is needed. */
public class RouteTests {
    private static void checkRoute(String name, String json, int target,
                                   List<List<Integer>> allowedRoutes, Long expectedCost) {
        Map<String, Object> input = Json.parseRequest(json);
        Map<String, Object> answer = Algorithms.dag(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);
        int[] parents = (int[]) answer.get("parent_edge");
        Long[] distances = (Long[]) answer.get("distance");
        List<Integer> route = Algorithms.reconstructRoute(source, target, edges, parents);
        if (!allowedRoutes.contains(route)) {
            throw new AssertionError(name + ": wrong vertex route " + route);
        }
        List<Integer> selectedIds = new ArrayList<>();
        Long cost = null;
        if (!route.isEmpty()) {
            cost = 0L;
            for (int i = 1; i < route.size(); i++) {
                int u = route.get(i - 1);
                int v = route.get(i);
                int id = parents[v];
                if (id < 0 || edges[id][0] != u || edges[id][1] != v) {
                    throw new AssertionError(name + ": incorrect edge direction");
                }
                selectedIds.add(id);
                cost += edges[id][2];
            }
        }
        if (!java.util.Objects.equals(cost, expectedCost)
                || !java.util.Objects.equals(distances[target], expectedCost)) {
            throw new AssertionError(name + ": incorrect route cost");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("source", source);
        result.put("target", target);
        result.put("route", route);
        result.put("parent_edge", parents);
        result.put("selected_edge_ids", selectedIds);
        result.put("cost", cost);
        result.put("passed", true);
        System.out.println(Json.write(result));
    }

    public static void main(String[] args) {
        String routes = "{\"task\":\"dag\",\"n\":6,\"source\":0,\"edges\":"
                + "[[0,1,3],[0,2,8],[1,2,-7],[2,3,4],[1,3,10],[4,5,-9]]}";
        checkRoute("reachable", routes, 3, List.of(List.of(0, 1, 2, 3)), 0L);
        checkRoute("source", routes, 0, List.of(List.of(0)), 0L);
        checkRoute("unreachable", routes, 4, List.of(Collections.emptyList()), null);
        String tied = "{\"task\":\"dag\",\"n\":5,\"source\":0,\"edges\":"
                + "[[0,1,2],[0,1,2],[0,2,1],[1,3,-3],[2,3,-2]]}";
        checkRoute("tied-paths", tied, 3, List.of(List.of(0, 1, 3), List.of(0, 2, 3)), -1L);
        checkRoute("parallel-tied-edges", tied, 1, List.of(List.of(0, 1)), 2L);
        String parallel = "{\"task\":\"dag\",\"n\":3,\"source\":0,\"edges\":"
                + "[[0,1,4],[0,1,-2],[1,2,5]]}";
        checkRoute("parallel-cheapest-edge", parallel, 2, List.of(List.of(0, 1, 2)), 3L);

        int n = 100000;
        int[][] edges = new int[n - 1][3];
        int[] parents = new int[n];
        Arrays.fill(parents, -1);
        for (int i = 1; i < n; i++) {
            edges[i - 1] = new int[]{i - 1, i, 1};
            parents[i] = i - 1;
        }
        List<Integer> route = Algorithms.reconstructRoute(0, n - 1, edges, parents);
        if (route.size() != n) {
            throw new AssertionError("Deep route has the wrong length");
        }
        for (int i = 0; i < n; i++) {
            if (route.get(i) != i) {
                throw new AssertionError("Deep route contains the wrong vertex");
            }
        }
        System.out.println(Json.write(Map.of("name", "deep-route", "route_length", route.size(),
                "first", route.get(0), "last", route.get(n - 1), "passed", true)));
    }
}
