package org.cmoflon.ide.core;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CMoflonCoreActivator extends AbstractUIPlugin
{

   // The shared instance
   private static CMoflonCoreActivator plugin;
 

   private static String bundleId;

   /**
    * The constructor
    */
   public CMoflonCoreActivator()
   {
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
    */
   @Override
   public void start(BundleContext context) throws Exception
   {
      super.start(context);
      plugin = this;
      bundleId = context.getBundle().getSymbolicName();
   }

   /*
    * (non-Javadoc)
    * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
    */
   @Override
   public void stop(BundleContext context) throws Exception
   {
      plugin = null;
      super.stop(context);
   }

   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static CMoflonCoreActivator getDefault()
   {
      return plugin;
   }

   public static String getModuleID()
   {
      if (bundleId == null)
      {
         throw new NullPointerException();
      } else
      {
         return bundleId;
      }
   }

}
