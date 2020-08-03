package info.codesaway.bex;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public final class Activator extends AbstractUIPlugin {

	/*
	 * Notes
	 * https://github.com/eclipse/eclipse.platform.team/tree/master/examples/org.eclipse.compare.examples
	 */

	// The plug-in ID
	public static final String PLUGIN_ID = "info.codesaway.bex"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static boolean getToggleState(final String commandId) {
		// https://stackoverflow.com/a/23742598/12610042
		// https://web.archive.org/web/20180311233946/http://www.robertwloch.net:80/2011/01/eclipse-tips-tricks-label-updating-command-handler/

		// https://www.eclipse.org/forums/index.php/t/156292/
		// http://blog.eclipse-tips.com/2009/03/commands-part-6-toggle-radio-menu.html

		// https://eclipsesource.com/blogs/2009/01/15/toggling-a-command-contribution/
		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		if (commandService != null) {
			Command command = commandService.getCommand(commandId);
			// commandService.refreshElements(command.getId(), null);
			State state = command.getState("org.eclipse.ui.commands.toggleState");

			// Added to prevent NullPointerException in some cases
			if (state == null) {
				return false;
			}

			boolean currentState = (Boolean) state.getValue();

			return currentState;
		}

		// How to add command
		// https://stackoverflow.com/a/34450815/12610042

		return false;
	}

	public static boolean shouldUseEnhancedCompare() {
		return getToggleState("info.codesaway.bex.commands.enhancedCompare");
	}

	public static boolean ignoreComments() {
		return getToggleState("info.codesaway.bex.commands.ignoreComments");
	}

	public static boolean shouldShowBothSidesOfSubstitution() {
		return getToggleState("info.codesaway.bex.commands.showBothSidesOfSubstitution");
	}
}
