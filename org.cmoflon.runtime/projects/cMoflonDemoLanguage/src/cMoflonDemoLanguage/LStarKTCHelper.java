package cMoflonDemoLanguage;

public final class LStarKTCHelper
{
   public static boolean evaluateHopcountConstraint(final int hopCount1, final int hopCount2, final int hopCount3, final double stretchFactor)
   {
      if (Math.min(hopCount1, Math.min(hopCount2, hopCount3)) < 0)
         return false;
      boolean result = true;
      result &= (!(hopCount1 == hopCount2) || true);
      result &= (!(hopCount1 > hopCount2) || ((hopCount3 + 1) * 1.0 / Math.max(1, hopCount1) < stretchFactor));
      result &= (!(hopCount1 < hopCount2) || ((hopCount3 + 1) * 1.0 / Math.max(1, hopCount2) < stretchFactor));
      return result;
   }
}
