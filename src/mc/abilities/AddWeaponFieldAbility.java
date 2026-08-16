package mc.abilities;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

public class AddWeaponFieldAbility extends Ability {
  /** 作用范围（世界单位） */
  public float range;
  /** 武器持续时间（帧） */
  public float duration;
  /** 添加武器后的冷却时间（帧），冷却期间不会给新单位添加武器 */
  public float cooldown;
  /** 添加的武器 */
  public Weapon weapon;
  /** 检测间隔（帧），避免每帧都检测 */
  public float checkInterval = 15f;

  // 运行时数据
  private transient float timer;
  /** 冷却计时器（帧），大于0表示正在冷却 */
  private transient float cooldownTimer;
  /** 本能力实例追踪的单位（用于画线、管理），不代表实际过期时间 */
  private transient ObjectMap<Unit, WeaponTimer> activeUnits = new ObjectMap<>();
  /** 临时列表用于清理 */
  private transient Seq<Unit> tmpRemove = new Seq<>();

  /** 全局共享：目标单位 -> (武器 -> 过期时间)，确保同一武器在同一目标上只存在一份 */
  private static final ObjectMap<Unit, ObjectMap<Weapon, Float>> globalExpireTimes = new ObjectMap<>();

  public AddWeaponFieldAbility() {
    super();
  }

  public AddWeaponFieldAbility(float range, float duration, float cooldown, Weapon weapon) {
    this.range = range;
    this.duration = duration;
    this.cooldown = cooldown;
    this.weapon = weapon;
  }

  @Override
  public void init(UnitType type) {
    super.init(type);
    if (weapon != null) {
      weapon.load();
      if (weapon.region == null || !weapon.region.found()) {
        weapon.region = Core.atlas.find("clear");
      }
    }
  }

  @Override
  public void update(Unit unit) {
    if (weapon == null)
      return;

    // 更新冷却计时器（帧）
    if (cooldownTimer > 0) {
      cooldownTimer -= Time.delta;
      if (cooldownTimer < 0)
        cooldownTimer = 0;
    }

    // 按间隔检测
    if ((timer += Time.delta) >= checkInterval) {
      timer = 0f;
      checkAndUpdateUnits(unit);
    }

    // 每帧更新计时器，清理过期或死亡的单位
    updateTimers();
  }

  private void checkAndUpdateUnits(Unit source) {
    if (cooldownTimer <= 0) {
      boolean[] addedAny = { false };
      Units.nearby(source.team, source.x, source.y, range, target -> {
        if (!target.isValid() || target == source)
          return;

        // 检查目标是否已有此武器
        boolean hasWeapon = false;
        for (WeaponMount mount : target.mounts) {
          if (mount.weapon == weapon) {
            hasWeapon = true;
            break;
          }
        }

        if (hasWeapon) {
          // 已有武器：不重复添加，仅重置全局过期时间，并纳入本能力追踪
          refreshWeaponTime(target);
          if (!activeUnits.containsKey(target)) {
            activeUnits.put(target, new WeaponTimer(Time.time + duration, -1));
          } else {
            activeUnits.get(target).expireTime = Time.time + duration;
          }
          return;
        }

        // 新单位：添加武器
        addWeaponToUnit(target);
        addedAny[0] = true;
      });

      // 若本次检测添加了新单位，启动冷却
      if (addedAny[0]) {
        cooldownTimer = cooldown;
      }
    } else {
      // 冷却期间：只刷新范围内已有该武器单位的时间
      Units.nearby(source.team, source.x, source.y, range, target -> {
        if (!target.isValid() || target == source)
          return;

        boolean hasWeapon = false;
        for (WeaponMount mount : target.mounts) {
          if (mount.weapon == weapon) {
            hasWeapon = true;
            break;
          }
        }

        if (hasWeapon) {
          refreshWeaponTime(target);
          if (activeUnits.containsKey(target)) {
            activeUnits.get(target).expireTime = Time.time + duration;
          }
        }
      });
    }
  }

  /** 刷新目标单位上本武器的全局过期时间 */
  private void refreshWeaponTime(Unit unit) {
    ObjectMap<Weapon, Float> unitWeapons = globalExpireTimes.get(unit);
    if (unitWeapons == null) {
      unitWeapons = new ObjectMap<>();
      globalExpireTimes.put(unit, unitWeapons);
    }
    unitWeapons.put(weapon, Time.time + duration);
  }

  private void updateTimers() {
    tmpRemove.clear();
    float now = Time.time;

    for (var entry : activeUnits) {
      Unit target = entry.key;

      // 单位死亡立即移除
      if (!target.isValid()) {
        tmpRemove.add(target);
        continue;
      }

      // 以全局过期时间为准
      ObjectMap<Weapon, Float> unitWeapons = globalExpireTimes.get(target);
      if (unitWeapons == null || !unitWeapons.containsKey(weapon)) {
        tmpRemove.add(target);
        continue;
      }

      if (now >= unitWeapons.get(weapon)) {
        tmpRemove.add(target);
      }
    }

    // 执行移除
    for (Unit target : tmpRemove) {
      removeWeaponFromUnit(target);

      // 清理全局记录
      ObjectMap<Weapon, Float> unitWeapons = globalExpireTimes.get(target);
      if (unitWeapons != null) {
        unitWeapons.remove(weapon);
        if (unitWeapons.isEmpty()) {
          globalExpireTimes.remove(target);
        }
      }

      activeUnits.remove(target);
    }
  }

  private void addWeaponToUnit(Unit unit) {
    WeaponMount[] old = unit.mounts;
    WeaponMount[] mounts = new WeaponMount[old.length + 1];
    System.arraycopy(old, 0, mounts, 0, old.length);

    WeaponMount newMount = weapon.mountType.get(weapon);

    if (old.length > 0) {
      newMount.aimX = old[0].aimX;
      newMount.aimY = old[0].aimY;
      newMount.shoot = old[0].shoot;
      newMount.rotate = old[0].rotate;
    } else {
      newMount.aimX = unit.aimX;
      newMount.aimY = unit.aimY;
    }

    mounts[old.length] = newMount;
    unit.mounts = mounts;

    // 写入全局过期时间
    refreshWeaponTime(unit);

    // 记录到本能力实例
    activeUnits.put(unit, new WeaponTimer(Time.time + duration, old.length));
  }

  private void removeWeaponFromUnit(Unit unit) {
    WeaponMount[] old = unit.mounts;
    if (old.length <= 0)
      return;

    int removeIndex = -1;
    for (int i = old.length - 1; i >= 0; i--) {
      if (old[i].weapon == weapon) {
        removeIndex = i;
        break;
      }
    }

    if (removeIndex < 0)
      return;

    WeaponMount[] mounts = new WeaponMount[old.length - 1];
    System.arraycopy(old, 0, mounts, 0, removeIndex);
    System.arraycopy(old, removeIndex + 1, mounts, removeIndex, old.length - removeIndex - 1);

    unit.mounts = mounts;
  }

  @Override
  public void draw(Unit unit) {
    Drawf.dashCircle(unit.x, unit.y, range, unit.team.color);

    for (var entry : activeUnits) {
      Unit target = entry.key;
      if (target.isValid()) {
        Draw.alpha(0.3f);
        Draw.color(unit.team.color);
        Lines.stroke(1f);
        Lines.line(unit.x, unit.y, target.x, target.y);
        Draw.alpha(1f);
      }
    }

    if (cooldownTimer > 0) {
      Draw.alpha(0.5f);
      Draw.color(unit.team.color);
      float progress = 1f - cooldownTimer / cooldown;
      Lines.stroke(2f);
      Lines.arc(unit.x, unit.y, range + 4f, progress, 90f);
      Draw.alpha(1f);
    }
  }

  @Override
  public void addStats(Table t) {
    super.addStats(t);
    t.add("[lightgray]范围: [stat]" + Strings.autoFixed(range / 8f, 1) + " [lightgray]格").row();
    t.add("[lightgray]持续时间: [stat]" + Strings.autoFixed(duration / 60f, 1) + " [lightgray]秒").row();
    t.add("[lightgray]冷却时间: [stat]" + Strings.autoFixed(cooldown / 60f, 1) + " [lightgray]秒").row();
    if (weapon != null) {
      mc.meta.CStatValues.displayWeapon(weapon, t);
    }
    t.row();
  }

  @Override
  public String localized() {
    return Core.bundle.get("ability.addweaponfieldability", "武器力场");
  }

  private static class WeaponTimer {
    public float expireTime;
    public int mountIndex;

    public WeaponTimer(float expireTime, int mountIndex) {
      this.expireTime = expireTime;
      this.mountIndex = mountIndex;
    }
  }
}
