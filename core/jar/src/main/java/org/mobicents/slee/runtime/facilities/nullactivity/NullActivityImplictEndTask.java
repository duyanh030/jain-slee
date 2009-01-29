package org.mobicents.slee.runtime.facilities.nullactivity;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.runtime.activity.ActivityContextState;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;

public class NullActivityImplictEndTask implements Runnable {

	private static final Logger logger = Logger.getLogger(NullActivityImplictEndTask.class);
	
	private final String acId;

	public NullActivityImplictEndTask(String acId) {
		super();
		this.acId = acId;
	}
	
	public void run() {
		
		SleeContainer sleeContainer = SleeContainer.lookupFromJndi();
		SleeTransactionManager txManager = sleeContainer.getTransactionManager();
		boolean rollback = true;
		try {
			txManager.begin();
			NullActivityContext ac = (NullActivityContext) sleeContainer.getActivityContextFactory().getActivityContext(acId,false);
			if (ac != null && ac.getState() == ActivityContextState.ACTIVE) {
				ac.implicitEndSecondCheck();
			}
			rollback = false;
		}
		catch (Exception e) {
			logger.error("failure while running implicit ending of null ac",e);								
		}
		finally {
			try {
				if (rollback) {
					txManager.rollback();
				}
				else {
					txManager.commit();
				}									
			}
			catch (Exception e) {
				logger.error("failure while ending tx, while running implicit ending of null ac",e);								
			}
		}
	}
}
