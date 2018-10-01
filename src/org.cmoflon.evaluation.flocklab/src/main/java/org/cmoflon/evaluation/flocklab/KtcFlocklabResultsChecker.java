package org.cmoflon.evaluation.flocklab;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.stream.SinkAdapter;
import org.graphstream.util.GraphDiff;

public class KtcFlocklabResultsChecker {

	private final File resultsFolder;
	private final double ktcKValue;

	public static void main(final String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: KtcFlocklabResultsChecker [results-folder-path] [kTC-k-value]");
			System.exit(1);
		}

		final File resultsFolder = new File(args[0]);
		final double ktcKValue = Double.parseDouble(args[1]);

		new KtcFlocklabResultsChecker(resultsFolder, ktcKValue).run();
	}

	public KtcFlocklabResultsChecker(final File resultsFolder, final double ktcKValue) {
		this.resultsFolder = resultsFolder;
		this.ktcKValue = ktcKValue;

		final File topologyBeforeFile = getTopologyBeforeFile();
		if (!topologyBeforeFile.exists()) {
			throw new IllegalArgumentException(String.format("Expected to find file %s", topologyBeforeFile));
		}

		final File topologyAfterFile = getTopologyAfterFile();
		if (!topologyAfterFile.exists()) {
			throw new IllegalArgumentException(String.format("Expected to find file %s", getTopologyAfterFile()));
		}
	}

	private void run() throws IOException {

		System.out.println("Evaluating " + this.resultsFolder);

		final Graph inputTopology = TextGraphReader.readGraph(getTopologyBeforeFile());
		final Graph actualOutputTopology = TextGraphReader.readGraph(getTopologyAfterFile());

		final Graph expectedOutputTopology = calculateVirtualTopology(inputTopology);

		System.out.println("Summary of involved graphs");
		printSummary(inputTopology);
		printSummary(actualOutputTopology);
		printSummary(expectedOutputTopology);

		System.out.println("Listing edges with differing weights in input and virtual topology...");
		for (final Edge inputTopologyEdge : inputTopology.getEdgeSet()) {
			final Edge outputTopologyEdge = actualOutputTopology.getEdge(inputTopologyEdge.getId());
			if (outputTopologyEdge != null) {
				final int inputWeight = TextGraphReader.getWeight(inputTopologyEdge);
				final int outputWeight = TextGraphReader.getWeight(outputTopologyEdge);
				if (inputWeight != outputWeight) {
					System.out.println(String.format("Weight of edge %s has changed: %d->%d", inputTopologyEdge.getId(),
							inputWeight, outputWeight));
				}
			}
		}
		System.out.println("Done");

		final GraphDiff graphDiff = new GraphDiff(expectedOutputTopology, actualOutputTopology);
		final List<String> edgeIdsOnlyInExpectedTopology = new ArrayList<>();
		final List<String> edgeIdsOnlyInActualTopology = new ArrayList<>();
		graphDiff.apply(new SinkAdapter() {

			@Override
			public void edgeRemoved(final String sourceId, final long timeId, final String edgeId) {
				edgeIdsOnlyInExpectedTopology.add(edgeId);
			}

			@Override
			public void edgeAdded(final String sourceId, final long timeId, final String edgeId,
					final String fromNodeId, final String toNodeId, final boolean directed) {
				edgeIdsOnlyInActualTopology.add(edgeId);
			}
		});

		System.out.println(String.format("Edges only in expected topology: #%d %s",
				edgeIdsOnlyInExpectedTopology.size(), edgeIdsOnlyInExpectedTopology));
		System.out.println(String.format("Edges only in actual topology:   #%d %s", edgeIdsOnlyInActualTopology.size(),
				edgeIdsOnlyInActualTopology));

	}

	private static boolean hasReverseEdge(final Edge edge) {
		final Node sourceNode = edge.getSourceNode();
		final Node targetNode = edge.getTargetNode();
		final boolean hasReverseEdge = targetNode.getEdgeToward(sourceNode) != null;
		return hasReverseEdge;
	}

	private Graph calculateVirtualTopology(final Graph inputTopology) {
		final Graph virtualTopology = Graphs.clone(inputTopology);
		TextGraphReader.setName(virtualTopology, "Virtual Topology");
		int triangleCounter = 0;
		for (final Node node1 : inputTopology.getNodeSet()) {
			for (final Edge edge12 : node1.<Edge>getEachLeavingEdge()) {
				final Node node2 = edge12.getTargetNode();
				if (node2.equals(node1)) {
					continue;
				}

				for (final Edge edge13 : node1.<Edge>getEachLeavingEdge()) {
					final Node node3 = edge13.getTargetNode();
					if (node3.equals(node1) || node3.equals(node2)) {
						continue;
					}

					final Edge edge32 = node3.getEdgeToward(node2);
					if (edge32 != null) {
						final int weight12 = TextGraphReader.getWeight(edge12);
						final int weight13 = TextGraphReader.getWeight(edge13);
						final int weight32 = TextGraphReader.getWeight(edge32);
						if (Arrays.asList(weight12, weight13, weight32).stream().anyMatch(w -> w == 0)) {
							continue;
						}
						if (weight12 > Math.max(weight13, weight32)
								&& weight12 > this.ktcKValue * Math.min(weight13, weight32)) {

							System.out.println(String.format("Triangle #%04d %6s[w=%d] %6s[w=%d] %6s[w=%d]",
									++triangleCounter, edge12.getId(), weight12, edge13.getId(), weight13,
									edge32.getId(), weight32));

							final Edge edgeInVirtualTopology = virtualTopology.getEdge(edge12.getId());
							if (edgeInVirtualTopology != null) {
								virtualTopology.removeEdge(edgeInVirtualTopology);
							}
						}
					}
				}
			}
		}

		makeSymmetric(virtualTopology);

		return virtualTopology;
	}

	private static boolean isSymmetric(final Graph flocklabInputTopology) {
		return flocklabInputTopology.getEdgeSet().stream().allMatch(edge -> hasReverseEdge(edge));
	}

	private static void makeSymmetric(final Graph graph) {
		final List<Edge> unidirectionalEdges = graph.getEdgeSet().stream().filter(edge -> !hasReverseEdge(edge))
				.collect(Collectors.toList());
		for (final Edge edge : unidirectionalEdges) {
			graph.removeEdge(edge);
		}
	}

	private File getTopologyBeforeFile() {
		return new File(resultsFolder, "output/01_TopologyBefore.txt");
	}

	private File getTopologyAfterFile() {
		return new File(resultsFolder, "output/02_TopologyAfter.txt");
	}

	private void printSummary(final Graph graph) {
		System.out.println(String.format("%20s n=%d m=%d isSymmetric=%b", TextGraphReader.getName(graph),
				graph.getNodeCount(), graph.getEdgeCount(), isSymmetric(graph)));
	}
}
