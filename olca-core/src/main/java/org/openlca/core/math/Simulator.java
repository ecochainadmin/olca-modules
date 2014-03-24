package org.openlca.core.math;

import org.openlca.core.matrix.ImpactMatrix;
import org.openlca.core.matrix.ImpactTable;
import org.openlca.core.matrix.Inventory;
import org.openlca.core.matrix.InventoryMatrix;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.results.SimpleResult;
import org.openlca.core.results.SimulationResult;
import org.openlca.expressions.FormulaInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A calculator for Monte-Carlo-Simulations.
 */
public class Simulator {

	private Logger log = LoggerFactory.getLogger(getClass());

	private ImpactMethodDescriptor impactMethod;
	private MatrixCache cache;
	private final IMatrixFactory<?> factory;
	private final IMatrixSolver matrixSolver;

	private SimulationResult result;
	private Inventory inventory;
	private ParameterTable parameterTable;
	private InventoryMatrix inventoryMatrix;
	private ImpactTable impactTable;
	private ImpactMatrix impactMatrix;
	private CalculationSetup setup;

	public Simulator(CalculationSetup setup, MatrixCache database,
			IMatrixSolver solver) {
		this.impactMethod = setup.getImpactMethod();
		this.cache = database;
		this.setup = setup;
		this.factory = solver.getMatrixFactory();
		this.matrixSolver = solver;
	}

	public SimulationResult getResult() {
		return result;
	}

	/**
	 * Generates random numbers and calculates the product system. Returns true
	 * if the calculation was successfully done, otherwise false (this is the
	 * case when the resulting matrix is singular).
	 */
	public boolean nextRun() {
		if (inventory == null || inventoryMatrix == null)
			setUp();
		try {
			log.trace("next simulation run");
			FormulaInterpreter interpreter = parameterTable.simulate();
			inventory.simulate(inventoryMatrix, interpreter);
			if (impactMatrix != null)
				impactTable.simulate(impactMatrix, interpreter);
			LcaCalculator solver = new LcaCalculator(matrixSolver);
			SimpleResult inventoryResult = solver.calculateFull(
					inventoryMatrix, impactMatrix);
			appendResults(inventoryResult);
			return true;
		} catch (Throwable e) {
			log.trace("simulation run failed", e);
			return false;
		}
	}

	private void appendResults(SimpleResult result) {
		this.result.appendFlowResults(result.getTotalFlowResults());
		if (this.result.hasImpactResults())
			this.result.appendImpactResults(result.getTotalImpactResults());
	}

	private void setUp() {
		log.trace("set up inventory");
		inventory = Calculators.createInventory(setup, cache);
		parameterTable = Calculators.createParameterTable(cache.getDatabase(),
				setup, inventory);
		inventoryMatrix = inventory.createMatrix(factory);
		result = new SimulationResult();
		result.setProductIndex(inventory.getProductIndex());
		result.setFlowIndex(inventory.getFlowIndex());
		if (impactMethod != null) {
			ImpactTable impactTable = ImpactTable.build(cache,
					impactMethod.getId(), inventory.getFlowIndex());
			if (impactTable.isEmpty()) {
				return;
			}
			this.impactTable = impactTable;
			this.impactMatrix = impactTable.createMatrix(factory);
			result.setImpactIndex(impactTable.getCategoryIndex());
		}
	}
}
