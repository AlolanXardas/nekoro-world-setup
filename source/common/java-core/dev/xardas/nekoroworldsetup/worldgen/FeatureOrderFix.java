package dev.xardas.nekoroworldsetup.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The same ordering as vanilla FeatureSorter.buildFeaturesPerStep, but a loop does not crash the game:
 * an arrow that would close a loop is skipped. This only changes which of the disagreeing features
 * (usually flowers and grass from different biome mods) is placed first.
 */
public final class FeatureOrderFix {
    private record Node(int index, int step, PlacedFeature feature) { }

    private static final Comparator<Node> ORDER = Comparator.comparingInt(Node::step).thenComparingInt(Node::index);

    private FeatureOrderFix() { }

    public static <T> List<FeatureSorter.StepFeatureData> sort(List<T> sources, Function<T, List<HolderSet<PlacedFeature>>> toFeatures) {
        Map<PlacedFeature, Integer> indexes = new HashMap<>();
        Map<Node, Set<Node>> edges = new TreeMap<>(ORDER);
        int steps = 0;
        for (T source : sources) {
            List<Node> order = new ArrayList<>();
            List<HolderSet<PlacedFeature>> perStep = toFeatures.apply(source);
            steps = Math.max(steps, perStep.size());
            for (int step = 0; step < perStep.size(); step++) {
                for (Holder<PlacedFeature> holder : perStep.get(step)) {
                    PlacedFeature feature = holder.value();
                    order.add(new Node(indexes.computeIfAbsent(feature, f -> indexes.size()), step, feature));
                }
            }
            for (int i = 0; i < order.size(); i++) {
                Set<Node> next = edges.computeIfAbsent(order.get(i), n -> new TreeSet<>(ORDER));
                if (i < order.size() - 1) next.add(order.get(i + 1));
            }
        }

        Set<Node> done = new TreeSet<>(ORDER);
        Set<Node> inProgress = new TreeSet<>(ORDER);
        List<Node> sorted = new ArrayList<>();
        for (Node node : edges.keySet()) visit(node, edges, done, inProgress, sorted);
        Collections.reverse(sorted);

        ImmutableList.Builder<FeatureSorter.StepFeatureData> result = ImmutableList.builder();
        for (int step = 0; step < steps; step++) {
            int s = step;
            List<PlacedFeature> features = sorted.stream().filter(n -> n.step() == s).map(Node::feature).toList();
            result.add(new FeatureSorter.StepFeatureData(features, Util.createIndexIdentityLookup(features)));
        }
        return result.build();
    }

    private static void visit(Node node, Map<Node, Set<Node>> edges, Set<Node> done, Set<Node> inProgress, List<Node> sorted) {
        // a node already in progress means this arrow closes a loop; vanilla crashes here, we skip the arrow
        if (done.contains(node) || inProgress.contains(node)) return;
        inProgress.add(node);
        for (Node next : edges.getOrDefault(node, Set.of())) visit(next, edges, done, inProgress, sorted);
        inProgress.remove(node);
        done.add(node);
        sorted.add(node);
    }
}
