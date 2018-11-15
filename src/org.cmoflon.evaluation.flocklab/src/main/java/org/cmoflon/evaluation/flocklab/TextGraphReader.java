package org.cmoflon.evaluation.flocklab;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

public class TextGraphReader {

	private static final String GRAPH_NAME_ATTRIBUTE = "name";
	private static final String Y_ATTRIBUTE = "y";
	private static final String X_ATTRIBUTE = "x";
	private static final String WEIGHT_ATTRIBUTE = "weight";

	private static final Pattern NODE_PATTERN = Pattern.compile("NODE (\\d+) (\\d+) (\\d+)");
	private static final Pattern EDGE_PATTERN = Pattern.compile("EDGE (\\d+) (\\d+) (\\d+) (\\d+)");

	private static final int WEIGHT_MISSING = 0;

	public static Graph readGraph(final File graphTextFile) throws IOException {
		final List<String> inputTopologyLines = FileUtils.readLines(graphTextFile, "UTF-8");
		final Graph graph = new MultiGraph(graphTextFile.getName());
		graph.setAttribute(GRAPH_NAME_ATTRIBUTE, graphTextFile.getName());
		for (final String inputTopologyLine : inputTopologyLines) {
			if (inputTopologyLine.startsWith("NODE")) {
				final Matcher matcher = NODE_PATTERN.matcher(inputTopologyLine);
				if (matcher.matches()) {

					final String nodeId = matcher.group(1);
					final int xCoordinate = Integer.parseInt(matcher.group(2));
					final int yCoordinate = Integer.parseInt(matcher.group(3));

					final Node node = graph.addNode(nodeId);
					node.setAttribute(X_ATTRIBUTE, xCoordinate);
					node.setAttribute(Y_ATTRIBUTE, yCoordinate);

				} else {
					System.err.println("Cannot parse line " + inputTopologyLine);
				}
			} else if (inputTopologyLine.startsWith("EDGE")) {
				final Matcher matcher = EDGE_PATTERN.matcher(inputTopologyLine);
				if (matcher.matches()) {

					final String firstNodeId = matcher.group(1);
					final String secondNodeId = matcher.group(2);
					final int weightForward = Integer.parseInt(matcher.group(3));
					final int weightBackward = Integer.parseInt(matcher.group(4));

					createEdge(graph, firstNodeId, secondNodeId, weightForward);
					createEdge(graph, secondNodeId, firstNodeId, weightBackward);

				} else {
					System.err.println("Cannot parse line " + inputTopologyLine);
				}
			}
		}
		return graph;
	}

	private static void createEdge(final Graph graph, final String sourceNodeId, final String targetNodeId,
			final int weight) {
		if (weight != WEIGHT_MISSING) {
			final Edge edge = graph.addEdge(generateEdgeId(sourceNodeId, targetNodeId), sourceNodeId, targetNodeId,
					true);
			edge.setAttribute(WEIGHT_ATTRIBUTE, weight);
		}
	}

	public static int getWeight(final Edge edge) {
		return edge.getAttribute(WEIGHT_ATTRIBUTE);
	}

	public static int getX(final Node node) {
		return node.getAttribute(X_ATTRIBUTE);
	}

	public static int getY(final Node node) {
		return node.getAttribute(Y_ATTRIBUTE);
	}

	private static String generateEdgeId(final String firstNodeId, final String secondNodeId) {
		return String.format("%s->%s", firstNodeId, secondNodeId);
	}

	public static void setName(final Graph graph, final String name) {
		graph.setAttribute(GRAPH_NAME_ATTRIBUTE, name);
	}

	public static String getName(final Graph graph) {
		return graph.getAttribute(GRAPH_NAME_ATTRIBUTE);
	}

}
