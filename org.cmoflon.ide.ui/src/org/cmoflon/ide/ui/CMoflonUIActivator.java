package org.cmoflon.ide.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CMoflonUIActivator extends AbstractUIPlugin
{

   // The shared instance
   private static CMoflonUIActivator plugin;

   private static String bundleId;

   /**
    * The constructor
    */
   public CMoflonUIActivator()
   {
   }

   @Override
   public void start(final BundleContext context) throws Exception
   {
      super.start(context);
      plugin = this;
      bundleId = context.getBundle().getSymbolicName();

      // CoreActivator.getDefault().reconfigureLogging();

      // Configure logging for eMoflon
      //setUpLogging();
   }

   @Override
   public void stop(final BundleContext context) throws Exception
   {
      plugin = null;
      bundleId = null;
      super.stop(context);
   }

   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static CMoflonUIActivator getDefault()
   {
      return plugin;
   }

   public static String getModuleID()
   {
      if (bundleId == null)
         throw new NullPointerException();
      else
         return bundleId;
   }

}
