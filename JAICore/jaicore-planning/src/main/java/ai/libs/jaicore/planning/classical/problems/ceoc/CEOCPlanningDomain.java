package ai.libs.jaicore.planning.classical.problems.ceoc;

import java.util.ArrayList;
import java.util.Collection;

import ai.libs.jaicore.planning.classical.problems.strips.Operation;
import ai.libs.jaicore.planning.classical.problems.strips.PlanningDomain;

public class CEOCPlanningDomain extends PlanningDomain {

	public CEOCPlanningDomain(Collection<CEOCOperation> operations) {
		super(new ArrayList<Operation>(operations));
	}

}
