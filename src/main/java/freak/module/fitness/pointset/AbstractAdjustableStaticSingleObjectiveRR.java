package freak.module.fitness.pointset;

import freak.core.control.ScheduleInterface;
import freak.core.population.Genotype;

public abstract class AbstractAdjustableStaticSingleObjectiveRR extends AbstractStaticSingleObjectiveRobustRegressionFitness  {

	private AbstractAdjust abstractAdjust; 
	
	public AbstractAdjustableStaticSingleObjectiveRR(ScheduleInterface schedule, AbstractAdjust adjust){
		super(schedule);
		abstractAdjust = adjust;
	}

	public String getLongDescriptionForAdjust() {
		return "Whether to perform intercept adjustment at each step.";
	}
	

	public String getShortDescriptionForAdjust() {
		return "Adjust Intercept";
	}
	
	public void setPropertyAdjust(Boolean adjust){
		interceptAdjust = adjust.booleanValue();
	}
	
	public Boolean getPropertyAdjust(){
		return new Boolean(interceptAdjust);
	} 
	
	protected void adjustIntercept(Genotype genotype) {
		if (abstractAdjust != null){
			abstractAdjust.adjust(this,genotype);
		}
	}

	
}
