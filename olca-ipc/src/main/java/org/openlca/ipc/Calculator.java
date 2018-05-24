package org.openlca.ipc;

import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NwSetDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.results.SimpleResult;
import org.openlca.jsonld.Json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class Calculator {

	private final Server server;
	private final RpcRequest req;

	private Calculator(Server server, RpcRequest req) {
		this.server = server;
		this.req = req;
	}

	static RpcResponse doIt(Server server, RpcRequest req) {
		return new Calculator(server, req).run();
	}

	private RpcResponse run() {
		if (req == null || req.params == null || !req.params.isJsonObject())
			return Responses.invalidParams("No calculation setup given", req);
		JsonObject json = req.params.getAsJsonObject();
		String systemID = Json.getRefId(json, "productSystem");
		if (systemID == null)
			Responses.invalidParams("No product system ID", req);
		ProductSystem system = new ProductSystemDao(server.db).getForRefId(systemID);
		if (system == null)
			Responses.invalidParams("No product system found for @id=" + systemID, req);
		CalculationSetup setup = new CalculationSetup(system);
		method(json, setup);
		nwSet(json, setup);
		setup.withCosts = Json.getBool(json, "withCosts", false);
		setup.setAmount(Json.getDouble(json, "amount", system.targetAmount));
		parameters(json, setup);

		SystemCalculator calc = new SystemCalculator(
				MatrixCache.createEager(server.db), server.solver);
		SimpleResult r = calc.calculateSimple(setup);
		return null;
	}

	private void method(JsonObject json, CalculationSetup setup) {
		String id = Json.getRefId(json, "impactMethod");
		if (id == null)
			return;
		setup.impactMethod = new ImpactMethodDao(server.db)
				.getDescriptorForRefId(id);
	}

	private void nwSet(JsonObject json, CalculationSetup setup) {
		String id = Json.getRefId(json, "nwSet");
		if (id == null)
			return;
		setup.nwSet = new NwSetDao(server.db)
				.getDescriptorForRefId(id);
	}

	private void parameters(JsonObject json, CalculationSetup setup) {
		JsonArray array = Json.getArray(json, "parameterRedefs");
		if (array == null)
			return;
		for (JsonElement e : array) {
			if (!e.isJsonObject())
				continue;
			JsonObject obj = e.getAsJsonObject();
			String name = Json.getString(obj, "name");
			if (name == null)
				continue;
			ParameterRedef redef = new ParameterRedef();
			redef.setName(name);
			redef.setValue(Json.getDouble(obj, "value", 1));

			JsonObject context = Json.getObject(obj, "context");
			if (context == null) {
				// global parameter redefinition
				setup.parameterRedefs.add(redef);
				continue;
			}

			// set the context
			BaseDescriptor d = parameterContext(context);
			if (d == null)
				continue;
			redef.setContextId(d.getId());
			redef.setContextType(d.getModelType());
			setup.parameterRedefs.add(redef);
		}
	}

	private BaseDescriptor parameterContext(JsonObject context) {
		String type = Json.getString(context, "@type");
		String refId = Json.getString(context, "@id");
		if ("Process".equals(type)) {
			return new ProcessDao(server.db).getDescriptorForRefId(refId);
		} else if ("ImpactMethod".equals(type)) {
			return new ImpactMethodDao(server.db).getDescriptorForRefId(refId);
		}
		return null;
	}
}