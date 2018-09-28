package org.cmoflon.ide.core.runtime.builders;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.gervarro.eclipse.workspace.util.RelevantElementCollector;
import org.gervarro.eclipse.workspace.util.VisitorCondition;

/**
 * This class provides a view of a given {@link RelevantElementCollector} that
 * only returns at most one relevant resource ({@link #getRelevantResources()}.
 *
 * @author Roland Kluge - Initial implementation
 *
 */
public class SingleResourceRelevantElementCollector extends RelevantElementCollector {

	private final RelevantElementCollector delegateCollector;

	/**
	 * Instantiates this collector.
	 *
	 * @param delegateCollector
	 *            the {@link RelevantElementCollector} to query for relevant
	 *            resources
	 * @param visitorCondition
	 *            the same {@link VisitorCondition} as used for delegateCollector
	 * @param project
	 *            the same {@link IProject} as used for delegateCollector
	 */
	public SingleResourceRelevantElementCollector(final RelevantElementCollector delegateCollector,
			final VisitorCondition visitorCondition, final IProject project) {
		super(project, visitorCondition);
		this.delegateCollector = delegateCollector;
	}

	/**
	 * Returns the first relevant resource of the relevant resources of the delegate
	 * collector
	 */
	@Override
	public List<IResource> getRelevantResources() {
		final List<IResource> relevantResources = getDelegateCollector().getRelevantResources();
		if (relevantResources.isEmpty()) {
			return relevantResources;
		} else {
			return relevantResources.subList(0, 1);
		}
	}

	/**
	 * Returns the delegate collector
	 *
	 * @return the delegateCollector the delegate collector
	 */
	public RelevantElementCollector getDelegateCollector() {
		return delegateCollector;
	}
}
