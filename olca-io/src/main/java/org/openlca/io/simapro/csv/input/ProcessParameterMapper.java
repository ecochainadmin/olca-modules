package org.openlca.io.simapro.csv.input;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.openlca.simapro.csv.model.CalculatedParameterRow;
import org.openlca.simapro.csv.model.InputParameterRow;
import org.openlca.simapro.csv.model.process.ProcessBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessParameterMapper {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final ParameterDao dao;
	private final FormulaInterpreter interpreter;
	private Process process;

	private long nextScope = 0;

	public ProcessParameterMapper(IDatabase database) {
		this.dao = new ParameterDao(database);
		this.interpreter = initInterpreter(dao);
	}

	private FormulaInterpreter initInterpreter(ParameterDao dao) {
		FormulaInterpreter interpreter = new FormulaInterpreter();
		for (Parameter parameter : dao.getGlobalParameters()) {
			interpreter.bind(parameter.name,
					Double.toString(parameter.value));
		}
		return interpreter;
	}

	/**
	 * Maps the parameter of the SimaPro process to the given openLCA process and
	 * creates an interpreter scope for this process. The parameters of the process
	 * are bound to this scope and can be used in evaluations later.
	 */
	public long map(ProcessBlock block, Process process) {
		this.process = process;
		long scopeId = ++nextScope;
		Scope scope = interpreter.createScope(scopeId);
		for (InputParameterRow row : block.getInputParameters()) {
			Parameter p = Parameters.create(row, ParameterScope.PROCESS);
			process.parameters.add(p);
			scope.bind(p.name, Double.toString(p.value));
		}
		for (CalculatedParameterRow row : block.getCalculatedParameters()) {
			Parameter p = Parameters.create(row, ParameterScope.PROCESS);
			process.parameters.add(p);
			scope.bind(p.name, p.formula);
		}
		evalProcessParameters(scopeId);
		this.process = null;
		return scopeId;
	}

	private void evalProcessParameters(long scopeId) {
		Scope scope = interpreter.getScope(scopeId);
		for (Parameter param : process.parameters) {
			if (param.isInputParameter)
				continue;
			try {
				double val = scope.eval(param.name);
				param.value = val;
			} catch (Exception e) {
				log.error("failed to evaluate process parameter " + param
						+ "; set it as an input parameter with value 1", e);
				param.formula = null;
				param.value = (double) 1;
				param.isInputParameter = true;
				scope.bind(param.name, "1");
			}
		}
	}

	public double eval(String formula, long scopeId) {
		try {
			Scope scope = interpreter.getScope(scopeId);
			return scope.eval(formula);
		} catch (Exception e) {
			log.error("failed to evaluate formula " + formula
					+ "; set value to NaN", e);
			return Double.NaN;
		}
	}
}
