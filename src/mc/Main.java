package mc;

import arc.Events;
import mc.blocks.CoreUnitFactory;
import mc.content.CUnitCommands;
import mc.core.MoveCoreSystem;
import mc.game.MEventTypes.MapChangeEvent;
import mc.gen.EntityRegistry;
import mc.net.CCall;
import mc.type.CoreUnitType;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.maps.Map;
import mindustry.mod.ClassMap;
import mindustry.mod.Mod;

public class Main extends Mod {
  public static Map hereMap = null;

  @Override
  public void loadContent() {
    EntityRegistry.register();
    CCall.load();
    CUnitCommands.load();
    MoveCoreSystem.init();
    ClassMap.classes.put("MoveCoreUnitType", CoreUnitType.class);
    ClassMap.classes.put("CoreUnitFactory", CoreUnitFactory.class);
  }

  @Override
  public void init() {
    Events.run(Trigger.update, () -> {
      updateMap();
    });
    Events.on(MapChangeEvent.class, e -> {
      for (CoreUnitType core : CoreUnitType.coreTypes) {
        if (!Vars.state.rules.bannedUnits.contains(core)) {
          Vars.state.rules.bannedUnits.add(core);
        }
      }
    });
  }

  public void updateMap() {
    if (Vars.state.map != null && Vars.state.map != hereMap) {
      Events.fire(new MapChangeEvent(Vars.state.map));
      hereMap = Vars.state.map;
    }
  }
}
