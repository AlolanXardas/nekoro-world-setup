package dev.xardas.nekoroworldsetup.worldgen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import dev.xardas.nekoroworldsetup.NekoroWorldSetup;

/**
 * Explains a "Feature order cycle found" crash. Vanilla only names the biomes; this rebuilds the same graph
 * (every biome's features in order, an arrow from each feature to the next one) and logs the features that
 * form the loop, with a biome that puts each pair in that order. The feature ids show which mods disagree.
 */
public final class FeatureCycleReport {
    private record Node(int step, PlacedFeature feature) { }

    private FeatureCycleReport() { }

    public static <T> void log(List<T> sources, Function<T, List<HolderSet<PlacedFeature>>> toFeatures) {
        try {
            Map<Node, Set<Node>> edges = new LinkedHashMap<>();
            Map<Node, String> names = new HashMap<>();
            Map<String, String> edgeSource = new HashMap<>();
            for (T source : sources) {
                List<Node> order = new ArrayList<>();
                List<HolderSet<PlacedFeature>> steps = toFeatures.apply(source);
                for (int step = 0; step < steps.size(); step++) {
                    for (Holder<PlacedFeature> holder : steps.get(step)) {
                        Node node = new Node(step, holder.value());
                        names.putIfAbsent(node, holder.unwrapKey().map(k -> k.location().toString()).orElse("(inline feature)")
                                + " [step " + step + "]");
                        order.add(node);
                    }
                }
                for (int i = 0; i < order.size(); i++) {
                    Set<Node> next = edges.computeIfAbsent(order.get(i), n -> new HashSet<>());
                    if (i < order.size() - 1 && next.add(order.get(i + 1))) {
                        edgeSource.put(key(order.get(i), order.get(i + 1)), String.valueOf(source));
                    }
                }
            }
            List<Node> cycle = findCycle(edges);
            if (cycle.isEmpty()) {
                NekoroWorldSetup.LOGGER.error("Feature order cycle: could not rebuild the loop");
                return;
            }
            StringBuilder sb = new StringBuilder("Feature order cycle explained (each line: feature, then the biome that puts the next one after it):");
            for (int i = 0; i < cycle.size(); i++) {
                Node from = cycle.get(i);
                Node to = cycle.get((i + 1) % cycle.size());
                sb.append("\n  ").append(names.get(from)).append("  ->  next in ").append(edgeSource.get(key(from, to)));
            }
            sb.append("\n  (back to the first one)");
            NekoroWorldSetup.LOGGER.error(sb.toString());
        } catch (RuntimeException e) {
            NekoroWorldSetup.LOGGER.error("Feature order cycle: report failed", e);
        }
    }

    private static String key(Node a, Node b) {
        return System.identityHashCode(a.feature()) + "/" + a.step() + ">" + System.identityHashCode(b.feature()) + "/" + b.step();
    }

    /** Iterative depth-first search; returns the nodes of the first loop found, in order. */
    private static List<Node> findCycle(Map<Node, Set<Node>> edges) {
        Set<Node> done = new HashSet<>();
        for (Node start : edges.keySet()) {
            if (done.contains(start)) continue;
            Deque<Node> path = new ArrayDeque<>();
            Set<Node> onPath = new HashSet<>();
            Deque<Iterator<Node>> stack = new ArrayDeque<>();
            path.push(start);
            onPath.add(start);
            stack.push(edges.getOrDefault(start, Set.of()).iterator());
            while (!stack.isEmpty()) {
                Iterator<Node> it = stack.peek();
                if (it.hasNext()) {
                    Node next = it.next();
                    if (onPath.contains(next)) {
                        List<Node> loop = new ArrayList<>(path);
                        Collections.reverse(loop);
                        return loop.subList(loop.indexOf(next), loop.size());
                    }
                    if (!done.contains(next)) {
                        path.push(next);
                        onPath.add(next);
                        stack.push(edges.getOrDefault(next, Set.of()).iterator());
                    }
                } else {
                    stack.pop();
                    Node finished = path.pop();
                    onPath.remove(finished);
                    done.add(finished);
                }
            }
        }
        return List.of();
    }
}
