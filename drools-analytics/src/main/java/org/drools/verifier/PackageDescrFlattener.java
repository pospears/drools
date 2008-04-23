package org.drools.verifier;

import java.util.Collection;

import org.drools.base.evaluators.Operator;
import org.drools.lang.descr.AccessorDescr;
import org.drools.lang.descr.AccumulateDescr;
import org.drools.lang.descr.AndDescr;
import org.drools.lang.descr.BaseDescr;
import org.drools.lang.descr.CollectDescr;
import org.drools.lang.descr.ConditionalElementDescr;
import org.drools.lang.descr.DeclarativeInvokerDescr;
import org.drools.lang.descr.EvalDescr;
import org.drools.lang.descr.ExistsDescr;
import org.drools.lang.descr.FieldAccessDescr;
import org.drools.lang.descr.FieldBindingDescr;
import org.drools.lang.descr.FieldConstraintDescr;
import org.drools.lang.descr.ForallDescr;
import org.drools.lang.descr.FromDescr;
import org.drools.lang.descr.FunctionCallDescr;
import org.drools.lang.descr.LiteralRestrictionDescr;
import org.drools.lang.descr.MethodAccessDescr;
import org.drools.lang.descr.NotDescr;
import org.drools.lang.descr.OrDescr;
import org.drools.lang.descr.PackageDescr;
import org.drools.lang.descr.PatternDescr;
import org.drools.lang.descr.PatternSourceDescr;
import org.drools.lang.descr.PredicateDescr;
import org.drools.lang.descr.QualifiedIdentifierRestrictionDescr;
import org.drools.lang.descr.RestrictionConnectiveDescr;
import org.drools.lang.descr.ReturnValueRestrictionDescr;
import org.drools.lang.descr.RuleDescr;
import org.drools.lang.descr.VariableRestrictionDescr;
import org.drools.verifier.components.AnalyticsAccessorDescr;
import org.drools.verifier.components.AnalyticsAccumulateDescr;
import org.drools.verifier.components.AnalyticsClass;
import org.drools.verifier.components.AnalyticsCollectDescr;
import org.drools.verifier.components.AnalyticsComponent;
import org.drools.verifier.components.AnalyticsComponentType;
import org.drools.verifier.components.AnalyticsEvalDescr;
import org.drools.verifier.components.AnalyticsFieldAccessDescr;
import org.drools.verifier.components.AnalyticsFromDescr;
import org.drools.verifier.components.AnalyticsFunctionCallDescr;
import org.drools.verifier.components.AnalyticsMethodAccessDescr;
import org.drools.verifier.components.AnalyticsPredicateDescr;
import org.drools.verifier.components.AnalyticsRule;
import org.drools.verifier.components.Consequence;
import org.drools.verifier.components.Constraint;
import org.drools.verifier.components.Field;
import org.drools.verifier.components.LiteralRestriction;
import org.drools.verifier.components.OperatorDescr;
import org.drools.verifier.components.Pattern;
import org.drools.verifier.components.PatternPossibility;
import org.drools.verifier.components.QualifiedIdentifierRestriction;
import org.drools.verifier.components.ReturnValueRestriction;
import org.drools.verifier.components.RulePackage;
import org.drools.verifier.components.RulePossibility;
import org.drools.verifier.components.TextConsequence;
import org.drools.verifier.components.Variable;
import org.drools.verifier.components.VariableRestriction;
import org.drools.verifier.dao.AnalyticsData;

/**
 * @author Toni Rikkola
 * 
 */
class PackageDescrFlattener {

	private Solvers solvers = new Solvers();

	private AnalyticsData data;

	private RulePackage currentPackage = null;
	private AnalyticsRule currentRule = null;
	private Pattern currentPattern = null;
	private Constraint currentConstraint = null;
	private AnalyticsClass currentClass = null;
	private Field currentField = null;

	/**
	 * Adds packageDescr to given AnalyticsData object
	 * 
	 * @param packageDescr
	 *            PackageDescr that will be flattened.
	 * @param data
	 *            AnalyticsData where the flattened objects are added.
	 */
	public void addPackageDescrToData(PackageDescr packageDescr,
			AnalyticsData data) {

		this.data = data;

		flatten(packageDescr);

		formPossibilities();
	}

	private void flatten(Collection<?> descrs, AnalyticsComponent parent) {

		int orderNumber = 0;

		for (Object o : descrs) {
			BaseDescr descr = (BaseDescr) o;
			if (descr instanceof PackageDescr) {
				flatten((PackageDescr) descr);
			} else if (descr instanceof RuleDescr) {
				flatten((RuleDescr) descr, parent);
			} else if (descr instanceof PatternDescr) {
				flatten((PatternDescr) descr, parent, orderNumber);
			} else if (descr instanceof VariableRestrictionDescr) {
				flatten((VariableRestrictionDescr) descr, parent, orderNumber);
			} else if (descr instanceof FieldBindingDescr) {
				flatten((FieldBindingDescr) descr, parent, orderNumber);
			} else if (descr instanceof FieldConstraintDescr) {
				flatten((FieldConstraintDescr) descr, parent, orderNumber);
			} else if (descr instanceof RestrictionConnectiveDescr) {
				flatten((RestrictionConnectiveDescr) descr, parent, orderNumber);
			} else if (descr instanceof LiteralRestrictionDescr) {
				flatten((LiteralRestrictionDescr) descr, parent, orderNumber);
			} else if (descr instanceof ReturnValueRestrictionDescr) {
				flatten((ReturnValueRestrictionDescr) descr, parent,
						orderNumber);
			} else if (descr instanceof QualifiedIdentifierRestrictionDescr) {
				flatten((QualifiedIdentifierRestrictionDescr) descr, parent,
						orderNumber);
			} else if (descr instanceof FunctionCallDescr) {
				flatten((FunctionCallDescr) descr, parent, orderNumber);
			} else if (descr instanceof PredicateDescr) {
				flatten((PredicateDescr) descr, parent, orderNumber);
			} else if (descr instanceof AccessorDescr) {
				flatten((AccessorDescr) descr, parent, orderNumber);
			} else if (descr instanceof MethodAccessDescr) {
				flatten((MethodAccessDescr) descr, parent, orderNumber);
			} else if (descr instanceof FieldAccessDescr) {
				flatten((FieldAccessDescr) descr, parent, orderNumber);
			} else if (descr instanceof PatternSourceDescr) {
				flatten((PatternSourceDescr) descr, parent);
			} else if (descr instanceof ConditionalElementDescr) {
				flatten((ConditionalElementDescr) descr, parent, orderNumber);
			}

			orderNumber++;
		}
	}

	private AnalyticsComponent flatten(PatternSourceDescr descr,
			AnalyticsComponent parent) {
		if (descr instanceof AccumulateDescr) {
			return flatten((AccumulateDescr) descr, parent);
		} else if (descr instanceof CollectDescr) {
			return flatten((CollectDescr) descr, parent);
		} else if (descr instanceof FromDescr) {
			return flatten((FromDescr) descr, parent);
		}

		return null;
	}

	private AnalyticsComponent flatten(DeclarativeInvokerDescr descr,
			AnalyticsComponent parent) {
		if (descr instanceof AccessorDescr) {
			return flatten((AccessorDescr) descr, parent);
		} else if (descr instanceof FieldAccessDescr) {
			return flatten((FieldAccessDescr) descr, parent);
		} else if (descr instanceof FunctionCallDescr) {
			return flatten((FunctionCallDescr) descr, parent);
		} else if (descr instanceof MethodAccessDescr) {
			return flatten((MethodAccessDescr) descr, parent);
		}

		return null;
	}

	private void flatten(ConditionalElementDescr descr,
			AnalyticsComponent parent, int orderNumber) {
		if (descr instanceof AndDescr) {
			flatten((AndDescr) descr, parent, orderNumber);
		} else if (descr instanceof CollectDescr) {
			flatten((CollectDescr) descr, parent, orderNumber);
		} else if (descr instanceof EvalDescr) {
			flatten((EvalDescr) descr, parent, orderNumber);
		} else if (descr instanceof ExistsDescr) {
			flatten((ExistsDescr) descr, parent);
		} else if (descr instanceof ForallDescr) {
			flatten((ForallDescr) descr, parent);
		} else if (descr instanceof FromDescr) {
			flatten((FromDescr) descr, parent);
		} else if (descr instanceof NotDescr) {
			flatten((NotDescr) descr, parent);
		} else if (descr instanceof OrDescr) {
			flatten((OrDescr) descr, parent, orderNumber);
		}
	}

	private void flatten(ForallDescr descr, AnalyticsComponent parent) {
		solvers.startForall();
		flatten(descr.getDescrs(), parent);
		solvers.endForall();
	}

	private void flatten(ExistsDescr descr, AnalyticsComponent parent) {
		solvers.startExists();
		flatten(descr.getDescrs(), parent);
		solvers.endExists();
	}

	private void flatten(NotDescr descr, AnalyticsComponent parent) {
		solvers.startNot();
		flatten(descr.getDescrs(), parent);
		solvers.endNot();
	}

	/**
	 * End leaf
	 * 
	 * @param descr
	 * @return
	 */
	private AnalyticsFunctionCallDescr flatten(FunctionCallDescr descr,
			AnalyticsComponent parent, int orderNumber) {
		AnalyticsFunctionCallDescr functionCall = new AnalyticsFunctionCallDescr();
		functionCall.setName(descr.getName());
		functionCall.setArguments(descr.getArguments());
		functionCall.setOrderNumber(orderNumber);
		functionCall.setParent(parent);

		return functionCall;
	}

	/**
	 * End leaf
	 * 
	 * @param descr
	 * @return
	 */
	private AnalyticsPredicateDescr flatten(PredicateDescr descr,
			AnalyticsComponent parent, int orderNumber) {

		AnalyticsPredicateDescr predicate = new AnalyticsPredicateDescr();
		predicate.setRuleName(currentRule.getRuleName());
		predicate.setRuleId(currentRule.getId());
		predicate.setContent(descr.getContent().toString());
		predicate.setClassMethodName(descr.getClassMethodName());
		predicate.setOrderNumber(orderNumber);
		predicate.setParent(parent);

		data.add(predicate);

		return predicate;
	}

	/**
	 * End leaf
	 * 
	 * @param descr
	 * @return
	 */
	private AnalyticsEvalDescr flatten(EvalDescr descr,
			AnalyticsComponent parent, int orderNumber) {

		AnalyticsEvalDescr eval = new AnalyticsEvalDescr();
		eval.setRuleId(currentRule.getId());
		eval.setRuleName(currentRule.getRuleName());
		eval.setContent(descr.getContent().toString());
		eval.setClassMethodName(descr.getClassMethodName());
		eval.setOrderNumber(orderNumber);
		eval.setParent(parent);

		data.add(eval);

		return eval;
	}

	/**
	 * End leaf
	 * 
	 * @param descr
	 * @return
	 */
	private AnalyticsFromDescr flatten(FromDescr descr,
			AnalyticsComponent parent) {
		AnalyticsFromDescr from = new AnalyticsFromDescr();

		AnalyticsComponent ds = flatten(descr.getDataSource(), from);
		from.setDataSourceId(ds.getId());
		from.setDataSourceType(ds.getComponentType());
		from.setParent(parent);

		return from;
	}

	private AnalyticsAccumulateDescr flatten(AccumulateDescr descr,
			AnalyticsComponent parent) {
		AnalyticsAccumulateDescr accumulate = new AnalyticsAccumulateDescr();

		accumulate.setInputPatternId(flatten(descr.getInputPattern(),
				accumulate, 0));
		accumulate.setInitCode(descr.getInitCode());
		accumulate.setActionCode(descr.getActionCode());
		accumulate.setReverseCode(descr.getReverseCode());
		accumulate.setResultCode(descr.getResultCode());

		// XXX: Array seems to be always null.
		// accumulate.setDeclarations(descr.getDeclarations());

		accumulate.setClassName(descr.getClassName());
		accumulate.setExternalFunction(descr.isExternalFunction());
		accumulate.setFunctionIdentifier(descr.getFunctionIdentifier());
		accumulate.setExpression(descr.getExpression());
		accumulate.setParent(parent);

		return accumulate;
	}

	private AnalyticsCollectDescr flatten(CollectDescr descr,
			AnalyticsComponent parent) {
		AnalyticsCollectDescr collect = new AnalyticsCollectDescr();
		collect.setClassMethodName(descr.getClassMethodName());
		collect
				.setInsidePatternId(flatten(descr.getInputPattern(), collect, 0));
		collect.setParent(parent);

		return collect;
	}

	private AnalyticsAccessorDescr flatten(AccessorDescr descr,
			AnalyticsComponent parent, int orderNumber) {
		AnalyticsAccessorDescr accessor = new AnalyticsAccessorDescr();
		accessor.setOrderNumber(orderNumber);
		accessor.setParent(parent);
		// TODO: I wonder what this descr does.
		return accessor;
	}

	/**
	 * End leaf
	 * 
	 * @param descr
	 */
	private AnalyticsMethodAccessDescr flatten(MethodAccessDescr descr,
			AnalyticsComponent parent, int orderNumber) {
		AnalyticsMethodAccessDescr accessor = new AnalyticsMethodAccessDescr();
		accessor.setMethodName(descr.getMethodName());
		accessor.setArguments(descr.getArguments());
		accessor.setOrderNumber(orderNumber);
		accessor.setParent(parent);

		return accessor;
	}

	/**
	 * End leaf
	 * 
	 * @param descr
	 */
	private AnalyticsFieldAccessDescr flatten(FieldAccessDescr descr,
			AnalyticsComponent parent, int orderNumber) {
		AnalyticsFieldAccessDescr accessor = new AnalyticsFieldAccessDescr();
		accessor.setFieldName(descr.getFieldName());
		accessor.setArgument(descr.getArgument());
		accessor.setOrderNumber(orderNumber);
		accessor.setParent(parent);

		return accessor;
	}

	private void flatten(PackageDescr descr) {
		RulePackage rulePackage = data.getRulePackageByName(descr.getName());

		if (rulePackage == null) {
			rulePackage = new RulePackage();

			rulePackage.setName(descr.getName());
			data.add(rulePackage);
		}

		currentPackage = rulePackage;

		flatten(descr.getRules(), rulePackage);
	}

	private void flatten(RuleDescr descr, AnalyticsComponent parent) {

		AnalyticsRule rule = new AnalyticsRule();
		currentRule = rule;

		rule.setRuleName(descr.getName());
		rule.setRuleSalience(descr.getSalience());
		rule.setConsequence(flattenConsequence(rule, descr.getConsequence()));
		rule.setLineNumber(descr.getLine());
		rule.setPackageId(currentPackage.getId());
		rule.setParent(parent);

		data.add(rule);

		currentPackage.getRules().add(rule);

		solvers.startRuleSolver(rule);
		flatten(descr.getLhs(), rule, 0);
		solvers.endRuleSolver();
	}

	/**
	 * Creates verifier object from rule consequence. Currently only supports
	 * text based consequences.
	 * 
	 * @param o
	 *            Consequence object.
	 * @return Analytics object that implements the Consequence interface.
	 */
	private Consequence flattenConsequence(AnalyticsComponent parent, Object o) {

		TextConsequence consequence = new TextConsequence();

		String text = o.toString();

		/*
		 * Strip all comments out of the code.
		 */
		StringBuffer buffer = new StringBuffer(text);
		int commentIndex = buffer.indexOf("//");

		while (commentIndex != -1) {
			buffer = buffer.delete(commentIndex, buffer.indexOf("\n",
					commentIndex));
			commentIndex = buffer.indexOf("//");
		}

		text = buffer.toString();

		/*
		 * Strip all useless characters out of the code.
		 */
		text = text.replaceAll("\n", "");
		text = text.replaceAll("\r", "");
		text = text.replaceAll("\t", "");
		text = text.replaceAll(" ", "");

		consequence.setText(text);
		consequence.setRuleId(currentRule.getId());
		consequence.setRuleName(currentRule.getRuleName());
		consequence.setParent(parent);

		data.add(consequence);

		return consequence;
	}

	private void flatten(OrDescr descr, AnalyticsComponent parent,
			int orderNumber) {
		OperatorDescr operatorDescr = new OperatorDescr(OperatorDescr.Type.OR);
		operatorDescr.setOrderNumber(orderNumber);
		operatorDescr.setParent(parent);

		data.add(operatorDescr);

		solvers.startOperator(operatorDescr);
		flatten(descr.getDescrs(), operatorDescr);
		solvers.endOperator();
	}

	private void flatten(AndDescr descr, AnalyticsComponent parent,
			int orderNumber) {
		OperatorDescr operatorDescr = new OperatorDescr(OperatorDescr.Type.AND);
		operatorDescr.setOrderNumber(orderNumber);
		operatorDescr.setParent(parent);

		data.add(operatorDescr);

		solvers.startOperator(operatorDescr);
		flatten(descr.getDescrs(), operatorDescr);
		solvers.endOperator();
	}

	private int flatten(PatternDescr descr, AnalyticsComponent parent,
			int orderNumber) {

		AnalyticsClass clazz = data.getClassByPackageAndName(descr
				.getObjectType());
		if (clazz == null) {
			clazz = new AnalyticsClass();
			clazz.setName(descr.getObjectType());
			data.add(clazz);
		}
		currentClass = clazz;

		Pattern pattern = new Pattern();
		pattern.setRuleId(currentRule.getId());
		pattern.setRuleName(currentRule.getRuleName());
		pattern.setClassId(clazz.getId());
		pattern.setName(clazz.getName());
		pattern.setPatternNot(solvers.getRuleSolver().isChildNot());
		pattern.setPatternExists(solvers.getRuleSolver().isExists());
		pattern.setPatternForall(solvers.getRuleSolver().isForall());
		pattern.setOrderNumber(orderNumber);
		pattern.setParent(parent);

		data.add(pattern);
		currentPattern = pattern;

		if (descr.getIdentifier() != null) {
			Variable variable = new Variable();
			variable.setRuleId(currentRule.getId());
			variable.setRuleName(currentRule.getRuleName());
			variable.setName(descr.getIdentifier());

			variable.setObjectType(AnalyticsComponentType.CLASS);
			variable.setObjectId(clazz.getId());
			variable.setObjectName(descr.getObjectType());

			data.add(variable);
		}

		// flatten source.
		if (descr.getSource() != null) {
			AnalyticsComponent source = flatten(descr.getSource(), pattern);
			pattern.setSourceId(source.getId());
			pattern.setSourceType(source.getComponentType());
		} else {
			pattern.setSourceId(0);
			pattern.setSourceType(AnalyticsComponentType.NOTHING);
		}
		solvers.startPatternSolver(pattern);
		flatten(descr.getConstraint(), pattern, 0);
		solvers.endPatternSolver();

		return pattern.getId();
	}

	private void flatten(FieldConstraintDescr descr, AnalyticsComponent parent,
			int orderNumber) {

		Field field = data.getFieldByClassAndFieldName(currentClass.getName(),
				descr.getFieldName());
		if (field == null) {
			field = createField(descr.getFieldName(), descr.getLine(),
					currentClass.getId(), currentClass.getName());
			data.add(field);
		}
		currentField = field;

		Constraint constraint = new Constraint();

		constraint.setRuleId(currentRule.getId());
		constraint.setFieldId(currentField.getId());
		constraint.setFieldName(currentField.getName());
		constraint.setPatternId(currentPattern.getId());
		constraint.setPatternIsNot(currentPattern.isPatternNot());
		constraint.setFieldId(field.getId());
		constraint.setOrderNumber(orderNumber);
		constraint.setParent(parent);

		data.add(constraint);

		currentConstraint = constraint;

		flatten(descr.getRestriction(), constraint, 0);
	}

	private void flatten(RestrictionConnectiveDescr descr,
			AnalyticsComponent parent, int orderNumber) {
		// TODO: check.
		flatten(descr.getRestrictions(), parent);
	}

	/**
	 * End
	 * 
	 * @param descr
	 */
	private void flatten(FieldBindingDescr descr, AnalyticsComponent parent,
			int orderNumber) {

		Variable variable = new Variable();
		variable.setRuleId(currentRule.getId());
		variable.setRuleName(currentRule.getRuleName());
		variable.setName(descr.getIdentifier());
		variable.setOrderNumber(orderNumber);
		variable.setParent(parent);

		variable.setObjectType(AnalyticsComponentType.FIELD);

		data.add(variable);
	}

	/**
	 * End
	 * 
	 * Foo( bar == $bar )<br>
	 * $bar is a VariableRestrictionDescr
	 * 
	 * @param descr
	 */
	private void flatten(VariableRestrictionDescr descr,
			AnalyticsComponent parent, int orderNumber) {

		Variable variable = data.getVariableByRuleAndVariableName(currentRule
				.getRuleName(), descr.getIdentifier());
		VariableRestriction restriction = new VariableRestriction();

		restriction.setRuleId(currentRule.getId());
		restriction.setRuleName(currentRule.getRuleName());
		restriction.setPatternId(currentPattern.getId());
		restriction.setPatternIsNot(currentPattern.isPatternNot());
		restriction.setConstraintId(currentConstraint.getId());
		restriction.setFieldId(currentConstraint.getFieldId());
		restriction.setOperator(Operator.determineOperator(
				descr.getEvaluator(), descr.isNegated()));
		restriction.setVariable(variable);
		restriction.setOrderNumber(orderNumber);
		restriction.setParent(parent);

		// Set field value, if it is unset.
		currentField.setFieldType(Field.FieldType.VARIABLE);

		data.add(restriction);
		solvers.addRestriction(restriction);
	}

	/**
	 * End
	 * 
	 * @param descr
	 */
	private void flatten(ReturnValueRestrictionDescr descr,
			AnalyticsComponent parent, int orderNumber) {

		ReturnValueRestriction restriction = new ReturnValueRestriction();

		restriction.setRuleId(currentRule.getId());
		restriction.setRuleName(currentRule.getRuleName());
		restriction.setPatternId(currentPattern.getId());
		restriction.setPatternIsNot(currentPattern.isPatternNot());
		restriction.setConstraintId(currentConstraint.getId());
		restriction.setFieldId(currentConstraint.getFieldId());
		restriction.setOperator(Operator.determineOperator(
				descr.getEvaluator(), descr.isNegated()));
		restriction.setClassMethodName(descr.getClassMethodName());
		restriction.setContent(descr.getContent());
		restriction.setDeclarations(descr.getDeclarations());
		restriction.setOrderNumber(orderNumber);
		restriction.setParent(parent);

		data.add(restriction);
		solvers.addRestriction(restriction);

	}

	/**
	 * End
	 * 
	 * @param descr
	 */
	private void flatten(LiteralRestrictionDescr descr,
			AnalyticsComponent parent, int orderNumber) {

		LiteralRestriction restriction = new LiteralRestriction();

		restriction.setRuleId(currentRule.getId());
		restriction.setRuleName(currentRule.getRuleName());
		restriction.setRuleId(currentRule.getId());
		restriction.setPatternId(currentPattern.getId());
		restriction.setPatternIsNot(currentPattern.isPatternNot());
		restriction.setConstraintId(currentConstraint.getId());
		restriction.setFieldId(currentConstraint.getFieldId());
		restriction.setOperator(Operator.determineOperator(
				descr.getEvaluator(), descr.isNegated()));
		restriction.setValue(descr.getText());
		restriction.setOrderNumber(orderNumber);
		restriction.setParent(parent);

		// Set field value, if it is unset.
		currentField.setFieldType(restriction.getValueType());

		data.add(restriction);
		solvers.addRestriction(restriction);
	}

	/**
	 * End
	 * 
	 * @param descr
	 */
	private void flatten(QualifiedIdentifierRestrictionDescr descr,
			AnalyticsComponent parent, int orderNumber) {

		String text = descr.getText();
		Variable variable = data.getVariableByRuleAndVariableName(currentRule
				.getRuleName(), text.substring(0, text.indexOf(".")));

		QualifiedIdentifierRestriction restriction = new QualifiedIdentifierRestriction();

		restriction.setRuleId(currentRule.getId());
		restriction.setPatternId(currentPattern.getId());
		restriction.setPatternIsNot(currentPattern.isPatternNot());
		restriction.setConstraintId(currentConstraint.getId());
		restriction.setFieldId(currentConstraint.getFieldId());
		restriction.setOperator(Operator.determineOperator(
				descr.getEvaluator(), descr.isNegated()));
		restriction.setVariableId(variable.getId());
		restriction.setVariableName(text.substring(0, text.indexOf(".")));
		restriction.setVariablePath(text.substring(text.indexOf(".")));
		restriction.setOrderNumber(orderNumber);
		restriction.setParent(parent);

		// Set field value, if it is unset.
		currentField.setFieldType(Field.FieldType.VARIABLE);

		variable.setObjectType(AnalyticsComponentType.FIELD);

		data.add(restriction);
		solvers.addRestriction(restriction);
	}

	private Field createField(String fieldName, int line, int classId,
			String className) {
		Field field = new Field();
		field.setClassId(classId);
		field.setClassName(className);
		field.setName(fieldName);
		field.setLineNumber(line);

		currentClass.getFields().add(field);
		return field;
	}

	private void formPossibilities() {

		for (PatternPossibility possibility : solvers.getPatternPossibilities()) {
			data.add(possibility);
		}
		for (RulePossibility possibility : solvers.getRulePossibilities()) {
			data.add(possibility);
		}
	}
}