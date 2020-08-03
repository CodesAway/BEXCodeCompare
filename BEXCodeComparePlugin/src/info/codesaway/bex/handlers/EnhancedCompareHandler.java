package info.codesaway.bex.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import info.codesaway.bex.views.BEXView;

public final class EnhancedCompareHandler extends AbstractHandler {
	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		// Toggle the command we just clicked and refresh the compare
		HandlerUtil.toggleCommandState(event.getCommand());

		if (BEXView.getInstance() != null) {
			BEXView.getInstance().refreshMergeViewer();
		}
		return null;
	}
}
