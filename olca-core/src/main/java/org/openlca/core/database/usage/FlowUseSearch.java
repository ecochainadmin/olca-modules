package org.openlca.core.database.usage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.Query;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowUseSearch implements IUseSearch<FlowDescriptor> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private IDatabase database;

	FlowUseSearch(IDatabase database) {
		this.database = database;
	}

	@Override
	public List<BaseDescriptor> findUses(FlowDescriptor flow) {
		if (flow == null)
			return Collections.emptyList();
		List<BaseDescriptor> processes = findInProcesses(flow);
		List<BaseDescriptor> methods = findInMethods(flow);
		List<BaseDescriptor> descriptors = new ArrayList<>(processes.size()
				+ methods.size() + 2);
		descriptors.addAll(processes);
		descriptors.addAll(methods);
		return descriptors;
	}

	private List<BaseDescriptor> findInMethods(FlowDescriptor flow) {
		String jpql = "select m.id, m.name, m.description from ImpactMethod m"
				+ " join m.impactCategories cat join cat.impactFactors fac "
				+ " where fac.flow.id = :flowId";
		try {
			List<Object[]> results = Query.on(database).getAll(Object[].class,
					jpql, Collections.singletonMap("flowId", flow.getId()));
			List<BaseDescriptor> descriptors = new ArrayList<>();
			for (Object[] result : results) {
				ImpactMethodDescriptor d = new ImpactMethodDescriptor();
				d.setId((Long) result[0]);
				d.setName((String) result[1]);
				d.setDescription((String) result[2]);
				descriptors.add(d);
			}
			return descriptors;
		} catch (Exception e) {
			log.error("Failed to search processes with used flow", e);
			return Collections.emptyList();
		}
	}

	private List<BaseDescriptor> findInProcesses(FlowDescriptor flow) {
		String jpql = "select p.id, p.name, p.description, p.processType, p.infrastructureProcess, p.location.id, p.category.id "
				+ "from Process p join p.exchanges e where e.flow.id = :flowId";
		try {
			List<Object[]> results = Query.on(database).getAll(Object[].class,
					jpql, Collections.singletonMap("flowId", flow.getId()));
			List<BaseDescriptor> descriptors = new ArrayList<>();
			for (Object[] result : results) {
				ProcessDescriptor d = new ProcessDescriptor();
				d.setId((Long) result[0]);
				d.setName((String) result[1]);
				d.setDescription((String) result[2]);
				d.setProcessType((ProcessType) result[3]);
				d.setInfrastructureProcess((Boolean) result[4]);
				d.setLocation((Long) result[5]);
				d.setCategory((Long) result[6]);
				descriptors.add(d);
			}
			return descriptors;
		} catch (Exception e) {
			log.error("Failed to search processes with used flow", e);
			return Collections.emptyList();
		}
	}
}
