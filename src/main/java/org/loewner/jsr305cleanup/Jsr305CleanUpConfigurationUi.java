package org.loewner.jsr305cleanup;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class Jsr305CleanUpConfigurationUi implements ICleanUpConfigurationUI {

	private CleanUpOptions _options;

	@Override
	public void setOptions(CleanUpOptions options) {
		_options = options;
	}

	@Override
	public Composite createContents(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;
		createCheckbox(composite, "Replace @Nonnull-parameters annotations with @ParameterAreNonnullByDefault",
				Jsr305CleanUp.USE_PARAMETERS_ARE_NONNULL_BY_DEFAULT);
		createCheckbox(composite, "Replace @Nonnull return value annotations with @ReturnValuesAreNonnullByDefault",
				Jsr305CleanUp.USE_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT);
		return composite;
	}

	private void createCheckbox(final Composite composite, final String label, final String optionName) {
		final Button convertButton = new Button(composite, SWT.CHECK);
		convertButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
		convertButton.setText(label);
		convertButton.setSelection(_options.isEnabled(optionName));
		convertButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				_options.setOption(optionName,
						convertButton.getSelection() ? CleanUpOptions.TRUE : CleanUpOptions.FALSE);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				_options.setOption(optionName,
						convertButton.getSelection() ? CleanUpOptions.TRUE : CleanUpOptions.FALSE);
			}
		});
	}

	@Override
	public int getCleanUpCount() {
		return 2;
	}

	@Override
	public int getSelectedCleanUpCount() {
		int result = 0;
		if (_options.isEnabled(Jsr305CleanUp.USE_PARAMETERS_ARE_NONNULL_BY_DEFAULT)) {
			result++;
		}
		if (_options.isEnabled(Jsr305CleanUp.USE_RETURN_VALUES_ARE_NONNULL_BY_DEFAULT)) {
			result++;
		}
		return result;
	}

	@Override
	public String getPreview() {
		return "n/a";
	}

}
