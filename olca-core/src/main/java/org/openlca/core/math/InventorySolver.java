package org.openlca.core.math;

import org.openlca.core.matrices.ExchangeCell;
import org.openlca.core.matrices.ExchangeMatrix;
import org.openlca.core.matrices.ImpactMatrix;
import org.openlca.core.matrices.Inventory;
import org.openlca.core.matrices.InventoryMatrix;
import org.openlca.core.matrices.ProductIndex;
import org.openlca.core.results.AnalysisResult;
import org.openlca.core.results.InventoryResult;

public class InventorySolver {

	public InventoryResult solve(Inventory inventory) {
		return solve(inventory, null);
	}

	public InventoryResult solve(Inventory inventory, ImpactMatrix impactMatrix) {
		InventoryMatrix matrix = new InventoryMatrix();
		matrix.setFlowIndex(inventory.getFlowIndex());
		matrix.setProductIndex(inventory.getProductIndex());
		IMatrix enviMatrix = inventory.getInterventionMatrix()
				.createRealMatrix();
		matrix.setInterventionMatrix(enviMatrix);
		IMatrix techMatrix = inventory.getTechnologyMatrix().createRealMatrix();
		matrix.setTechnologyMatrix(techMatrix);
		return solve(matrix, impactMatrix);
	}

	public InventoryResult solve(InventoryMatrix matrix) {
		return solve(matrix, null);
	}

	public InventoryResult solve(InventoryMatrix matrix,
			ImpactMatrix impactMatrix) {
		IMatrix techMatrix = matrix.getTechnologyMatrix();
		IMatrix demand = Calculators.createDemandVector(matrix
				.getProductIndex());
		IMatrix s = techMatrix.solve(demand);
		IMatrix enviMatrix = matrix.getInterventionMatrix();
		IMatrix g = enviMatrix.multiply(s);

		InventoryResult result = new InventoryResult();
		result.setFlowIndex(matrix.getFlowIndex());
		result.setFlowResults(g.getColumn(0));
		result.setProductIndex(matrix.getProductIndex());
		result.setScalingFactors(s.getColumn(0));
		if (impactMatrix != null) {
			IMatrix impactFactors = impactMatrix.getValues();
			IMatrix i = impactFactors.multiply(g);
			result.setImpactIndex(impactMatrix.getCategoryIndex());
			result.setImpactResults(i.getColumn(0));
		}
		return result;
	}

	public AnalysisResult analyse(Inventory inventory) {
		return analyse(inventory, null);
	}

	public AnalysisResult analyse(Inventory inventory, ImpactMatrix impactMatrix) {

		ProductIndex productIndex = inventory.getProductIndex();
		ExchangeMatrix techExchanges = inventory.getTechnologyMatrix();
		int n = productIndex.size();

		AnalysisResult result = new AnalysisResult(inventory.getFlowIndex(),
				inventory.getProductIndex());

		IMatrix techMatrix = techExchanges.createRealMatrix();
		IMatrix enviMatrix = inventory.getInterventionMatrix()
				.createRealMatrix();
		IMatrix inverse = techMatrix.getInverse();

		IMatrix demand = Calculators.createDemandVector(productIndex);
		IMatrix scalingFactors = inverse.multiply(demand);
		// we now that the reference product is always in the first column
		result.setScalingFactors(scalingFactors.getColumn(0));

		// single results
		IMatrix scalingMatrix = MatrixFactory.create(n, n);
		for (int i = 0; i < n; i++) {
			scalingMatrix.setEntry(i, i, scalingFactors.getEntry(i, 0));
		}
		IMatrix singleResult = enviMatrix.multiply(scalingMatrix);
		result.setSingleResult(singleResult);

		// total results
		// TODO: loop correction
		IMatrix demandMatrix = MatrixFactory.create(n, n);
		for (int i = 0; i < productIndex.size(); i++) {
			ExchangeCell productCell = techExchanges.getEntry(i, i);
			if (productCell == null)
				continue;
			double amount = scalingFactors.getEntry(i, 0)
					* productCell.getMatrixValue();
			demandMatrix.setEntry(i, i, amount);
		}
		IMatrix totalResult = enviMatrix.multiply(inverse).multiply(
				demandMatrix);
		result.setTotalResult(totalResult);

		if (impactMatrix != null) {
			result.setImpactCategoryIndex(impactMatrix.getCategoryIndex());
			IMatrix factors = impactMatrix.getValues();
			result.setImpactFactors(factors);
			IMatrix singleImpactResult = factors.multiply(singleResult);
			result.setSingleImpactResult(singleImpactResult);
			IMatrix totalImpactResult = factors.multiply(totalResult);
			result.setTotalImpactResult(totalImpactResult);
		}
		return result;
	}

}