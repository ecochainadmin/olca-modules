package org.openlca.jsonld.input;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.jsonld.Json;
import org.openlca.util.RefIdMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Maps the reference exchange and exchange IDs of the process links of a
 * product system and generates the process links. This function should be
 * called at the end of a product system import when all the referenced data are
 * already imported.
 */
class ProductSystemLinks {

	private final Logger log = LoggerFactory
			.getLogger(ProductSystemLinks.class);

	private final IDatabase db;
	private final RefIdMap<String, Long> refIds;

	/**
	 * A map of maps (processID, internalID) -> exchangeID that maps the
	 * internal IDs of exchanges used in the JSON-LD to the exchangeIDs of the
	 * database.
	 */
	private final Map<Long, Map<Integer, Long>> exchangeIds;

	private ProductSystemLinks(ImportConfig conf) {
		db = conf.db.getDatabase();
		refIds = RefIdMap.refToInternal(
				db, ProductSystem.class, Process.class,
				Flow.class, Unit.class);
		exchangeIds = new HashMap<>();
		try {
			// TODO: this currently add *ALL* exchanges from the database
			// to the ID map but we could reduce this to add only exchanges
			// that can be linked (product inputs and waste outputs)
			String sql = "SELECT f_owner, id, internal_id FROM tbl_exchanges";
			NativeSql.on(db).query(sql, rs -> {
				long process = rs.getLong(1);
				long exchange = rs.getLong(2);
				int internalId = rs.getInt(3);
				Map<Integer, Long> ofProcess = exchangeIds.get(process);
				if (ofProcess == null) {
					exchangeIds.put(process, ofProcess = new HashMap<>());
				}
				ofProcess.put(internalId, exchange);
				return true;
			});
		} catch (SQLException e) {
			log.error("Error loading exchange ids", e);
		}
	}

	static void map(JsonObject json, ImportConfig conf, ProductSystem system) {
		new ProductSystemLinks(conf).map(json, system);
	}

	private void map(JsonObject json, ProductSystem system) {
		if (json == null || system == null)
			return;
		setReferenceExchange(json, system);
		JsonArray array = Json.getArray(json, "processLinks");
		if (array == null || array.size() == 0)
			return;
		for (JsonElement element : array) {
			JsonObject obj = element.getAsJsonObject();
			ProcessLink link = new ProcessLink();
			link.isSystemLink = Json.getBool(obj, "isSystemLink", false);
			if (link.isSystemLink) {
				link.providerId = getId(obj, "provider", ProductSystem.class);
			} else {
				link.providerId = getId(obj, "provider", Process.class);
			}
			link.processId = getId(obj, "process", Process.class);
			link.flowId = getId(obj, "flow", Flow.class);
			JsonObject exchange = Json.getObject(obj, "exchange");
			int internalId = Json.getInt(exchange, "internalId", 0);
			link.exchangeId = findExchangeId(link.processId, internalId);

			// add the link when it is valid
			if (link.providerId != 0
					&& link.processId != 0
					&& link.flowId != 0
					&& link.exchangeId != 0) {
				system.processLinks.add(link);
			}
		}
	}

	private void setReferenceExchange(JsonObject json, ProductSystem system) {
		Process refProcess = system.referenceProcess;
		if (refProcess == null)
			return;
		JsonObject refJson = Json.getObject(json, "referenceExchange");
		int internalId = Json.getInt(refJson, "internalId", 0);
		if (internalId <= 0)
			return;
		Exchange refExchange = refProcess.getExchange(internalId);
		system.referenceExchange = refExchange;
		system.targetFlowPropertyFactor = findFactor(json, system);
		system.targetUnit = findUnit(json, system);
	}

	private FlowPropertyFactor findFactor(JsonObject json, ProductSystem s) {
		Exchange e = s.referenceExchange;
		if (e == null)
			return null;
		String propertyRefId = Json.getRefId(json, "targetFlowProperty");
		for (FlowPropertyFactor f : e.flow.flowPropertyFactors)
			if (f.flowProperty.refId.equals(propertyRefId))
				return f;
		return null;
	}

	private Unit findUnit(JsonObject json, ProductSystem s) {
		FlowPropertyFactor f = s.targetFlowPropertyFactor;
		if (f == null)
			return null;
		String unitRefId = Json.getRefId(json, "targetUnit");
		UnitGroup ug = f.flowProperty.unitGroup;
		for (Unit u : ug.units)
			if (u.refId.equals(unitRefId))
				return u;
		return null;
	}

	private long getId(JsonObject json, String key, Class<?> type) {
		JsonObject refObj = Json.getObject(json, key);
		if (refObj == null)
			return 0;
		String refId = Json.getString(refObj, "@id");
		Long id = refIds.get(type, refId);
		return id == null ? 0 : id;
	}

	private long findExchangeId(long processId, int internalId) {
		Map<Integer, Long> ofProcess = exchangeIds.get(processId);
		if (ofProcess == null)
			return 0;
		if (ofProcess.get(internalId) == null)
			return 0;
		return ofProcess.get(internalId);
	}

}
