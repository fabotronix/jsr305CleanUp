package org.loewner.jsr305cleanup;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

public class Jsr305CleanUpOptionsInitializer implements ICleanUpOptionsInitializer {

	@Override
	public void setDefaultOptions(CleanUpOptions options) {
		options.setOption(Jsr305CleanUp.USE_PARAMETERS_ARE_NONNULL_BY_DEFAULT, CleanUpOptions.FALSE);
		options.setOption(Jsr305CleanUp.USE_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT, CleanUpOptions.FALSE);
	}

}
