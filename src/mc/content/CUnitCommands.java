package mc.content;

import mc.ai.type.CoreAuxiliaryAI;
import mc.ai.type.FleeAI;
import mindustry.ai.UnitCommand;

public class CUnitCommands {
  public static UnitCommand coreAuxiliaryCommand, flee;

  public static void load() {
    coreAuxiliaryCommand = new UnitCommand("core-auxiliary", "production", u -> {
      if (CoreAuxiliaryAI.canUse(u)) {
        return new CoreAuxiliaryAI();
      }
      return null;
    });
    flee = new UnitCommand("flee", "", u -> new FleeAI());
  }

}
