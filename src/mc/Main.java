package mc;

import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import ent.anno.Annotations.EntityDef;
import mc.abilities.AddWeaponAbility;
import mc.blocks.CoreUnderUnloader;
import mc.blocks.CoreUnitFactory;
import mc.blocks.MCoreBlock.MCoreBuild;
import mc.content.CUnitCommands;
import mc.content.MBlocks;
import mc.content.MUnits;
import mc.content.Tech;
import mc.core.MoveCoreSystem;
import mc.game.MEventTypes.MapChangeEvent;
import mc.gen.Corec;
import mc.gen.EntityRegistry;
import mc.gen.RetractableLegsc;
import mc.net.CCall;
import mc.type.CoreUnitType;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.gen.Mechc;
import mindustry.gen.Unit;
import mindustry.gen.Unitc;
import mindustry.maps.Map;
import mindustry.mod.ClassMap;
import mindustry.mod.Mod;
import mindustry.ui.Styles;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class Main extends Mod {
  public static @EntityDef({ Unitc.class, Corec.class, RetractableLegsc.class }) CoreUnitType core1;
  public static @EntityDef({ Unitc.class, Corec.class, Mechc.class }) CoreUnitType core2;

  public static Map hereMap = null;
  boolean ban = false;
  public static float timer = 0f;

  @Override
  public void loadContent() {
    MUnits.load();
    MBlocks.load();
    Tech.load();
    if (ban)
      Events.on(ClientLoadEvent.class, e -> {
        Vars.ui.hudGroup.fill(null, table -> {
          table.table(null, t -> {
            t.button("调试面板", Styles.flatt, () -> {
              Seq<Unit> allEnemies = new Seq<>();
              for (Unit u : Groups.unit) {
                if (u.team() == Vars.state.rules.waveTeam) {
                  allEnemies.add(u);
                }
              }
              Unit random = allEnemies.random();
              MCoreBuild b = (MCoreBuild) Vars.state.teams.closestEnemyCore(random.x, random.y,
                  Vars.state.rules.waveTeam);
              ;
              Log.info(
                  "[core]" + b);
              Log.info("[X]: " + b.x() + "[Y]: " + b.y());
            }).size(100, 70);

          }).size(100, 70);
          table.center().left().update(() -> {
            table.translation.set(1, 100);
          });
        });
      });
    EntityRegistry.register();
    CCall.load();
    CUnitCommands.load();
    MoveCoreSystem.init();
    ClassMap.classes.put("MoveCoreUnitType", CoreUnitType.class);
    ClassMap.classes.put("CoreUnitFactory", CoreUnitFactory.class);
    ClassMap.classes.put("CoreUnderUnloader", CoreUnderUnloader.class);
    ClassMap.classes.put("AddWeaponAbility", AddWeaponAbility.class);
  }

  @Override
  public void init() {
    Events.run(Trigger.update, () -> {
      updateMap();
      timer += Time.delta;
    });
    if (ban)
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
