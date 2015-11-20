package org.openlca.jsonld.input;

import java.util.List;

import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.NwFactor;
import org.openlca.core.model.NwSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class NwSets {

	static NwSet map(JsonObject json, List<ImpactCategory> categories) {
		if (json == null)
			return null;
		NwSet set = new NwSet();
		In.mapAtts(json, set);
		set.setWeightedScoreUnit(In.getString(json, "weightedScoreUnit"));
		JsonArray factors = In.getArray(json, "factors");
		if (factors == null)
			return set;
		for (JsonElement f : factors) {
			if (!f.isJsonObject())
				continue;
			NwFactor factor = mapFactor(f.getAsJsonObject(), categories);
			set.getFactors().add(factor);
		}
		return set;
	}

	private static NwFactor mapFactor(JsonObject json,
			List<ImpactCategory> categories) {
		NwFactor f = new NwFactor();
		String categoryId = In.getRefId(json, "impactCategory");
		f.setImpactCategory(getImpactCategory(categoryId, categories));
		f.setNormalisationFactor(In.getDouble(json, "normalisationFactor", 0));
		f.setWeightingFactor(In.getDouble(json, "weightingFactor", 0));
		return f;
	}

	private static ImpactCategory getImpactCategory(String refId,
			List<ImpactCategory> categories) {
		for (ImpactCategory category : categories)
			if (category.getRefId().equals(refId))
				return category;
		return null;
	}

}
