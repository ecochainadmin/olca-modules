package org.openlca.io.xls.results;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.openlca.core.database.EntityCache;
import org.openlca.core.model.ProjectVariant;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.io.CategoryPair;
import org.openlca.util.Strings;

public class Sort {

	public static List<ImpactCategoryDescriptor> impacts(
			Collection<ImpactCategoryDescriptor> impacts) {
		List<ImpactCategoryDescriptor> list = new ArrayList<>(impacts);
		Collections.sort(list, new Comparator<ImpactCategoryDescriptor>() {
			@Override
			public int compare(ImpactCategoryDescriptor o1,
					ImpactCategoryDescriptor o2) {
				return Strings.compare(o1.name, o2.name);
			}
		});
		return list;
	}

	public static List<ProjectVariant> variants(
			Collection<ProjectVariant> variants) {
		List<ProjectVariant> list = new ArrayList<>(variants);
		Collections.sort(list, new Comparator<ProjectVariant>() {
			@Override
			public int compare(ProjectVariant o1, ProjectVariant o2) {
				return Strings.compare(o1.name, o2.name);
			}
		});
		return list;
	}

	public static List<CategorizedDescriptor> processes(
			Collection<CategorizedDescriptor> processes, long refProcessId) {
		List<CategorizedDescriptor> list = new ArrayList<>(processes);
		Collections.sort(list, new Comparator<CategorizedDescriptor>() {
			@Override
			public int compare(CategorizedDescriptor o1,
					CategorizedDescriptor o2) {
				if (o1.id == refProcessId)
					return -1;
				if (o2.id == refProcessId)
					return 1;
				return Strings.compare(o1.name, o2.name);
			}
		});
		return list;
	}

	public static List<FlowDescriptor> flows(Collection<FlowDescriptor> flows,
			EntityCache cache) {
		if (flows == null)
			return Collections.emptyList();
		ArrayList<FlowDescriptor> sorted = new ArrayList<>(flows);
		Collections.sort(sorted, new FlowSorter(cache));
		return sorted;
	}

	private static class FlowSorter implements Comparator<FlowDescriptor> {

		private HashMap<Long, CategoryPair> flowCategories = new HashMap<>();
		private EntityCache cache;

		private FlowSorter(EntityCache cache) {
			this.cache = cache;
		}

		@Override
		public int compare(FlowDescriptor o1, FlowDescriptor o2) {
			CategoryPair cat1 = flowCategory(o1);
			CategoryPair cat2 = flowCategory(o2);
			int c = cat1.compareTo(cat2);
			if (c != 0)
				return c;
			return Strings.compare(o1.name, o2.name);
		}

		private CategoryPair flowCategory(FlowDescriptor flow) {
			CategoryPair pair = flowCategories.get(flow.id);
			if (pair != null)
				return pair;
			pair = CategoryPair.create(flow, cache);
			flowCategories.put(flow.id, pair);
			return pair;
		}
	}
}
