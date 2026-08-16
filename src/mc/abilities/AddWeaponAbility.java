package mc.abilities;

import arc.Core;
import arc.math.Angles;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import mc.meta.CStatValues;
import mindustry.entities.Predict;
import mindustry.entities.Sized;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Healthc;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

public class AddWeaponAbility extends Ability {
  public float per;
  public Weapon weapon;
  public boolean added = false;
  protected @Nullable Teamc target;
  protected @Nullable Teamc bomberTarget;
  protected static final float rotateBackTimer = 60f * 5f;
  protected Interval timer = new Interval(4);
  protected static final int timerTarget = 0;
  protected float noTargetTime;

  public AddWeaponAbility(float per, Weapon weapon) {
    this.per = per;
    this.weapon = weapon;
  }

  public AddWeaponAbility() {
  }

  float time = 0;

  @Override
  public void update(Unit unit) {
    time++;
    Seq<WeaponMount> mounts = new Seq<>(unit.mounts);
    if (!mounts.contains(w -> w.weapon == this.weapon)) {
      added = false;
    }
    if (unit.healthf() <= per && added == false && weapon != null) {
      if (weapon.region == null || !weapon.region.found()) {
        weapon.load();
      }
      Seq<Weapon> weapons = new Seq<>(unit.type.weapons);
      weapons.add(weapon);
      unit.mounts = new WeaponMount[weapons.size];
      for (int i = 0; i < unit.mounts.length; i++) {
        unit.mounts[i] = weapons.get(i).mountType.get(weapons.get(i));
      }
      added = true;
    }
    if (added) {
      // 玩家正在射击时不干预，让 controlWeapons 的输入生效
      // 玩家未射击或非玩家控制时，自动索敌开火
      boolean playerShooting = unit.isPlayer() && unit.getPlayer().shooting;
      if (!playerShooting) {
        updateWeapons(unit);
      }
    }
  }

  @Override
  public String localized() {
    return Core.bundle.get("ability.addweaponability");
  }

  @Override
  public void addStats(Table t) {
    super.addStats(t);
    t.add("血量低于" + per * 100 + "%时添加武器").row();
    CStatValues.displayWeapon(weapon, t);
    t.row();
  }

  public Teamc target(Unit unit, float x, float y, float range, boolean air, boolean ground) {
    return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground),
        t -> ground && (unit.type.targetUnderBlocks || !t.block.underBullets));
  }

  public boolean retarget() {
    return timer.get(timerTarget, target == null ? 40 : 90);
  }

  public Teamc findMainTarget(Unit unit, float x, float y, float range, boolean air, boolean ground) {
    return findTarget(unit, x, y, range, air, ground);
  }

  public Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground) {
    return target(unit, x, y, range, air, ground);
  }

  public boolean invalid(Unit unit, Teamc target) {
    return Units.invalidateTarget(target, unit.team, unit.x, unit.y);
  }

  public void targetInvalidated() {
    // immediately find a new target
    timer.reset(timerTarget, -1f);
  }

  public void updateWeapons(Unit unit) {
    float rotation = unit.rotation - 90;
    boolean ret = retarget();

    // 用武器实际射程而非 unit.range()（后者在 weapons 为空时回退为 mineRange=70）
    float weaponRange = weapon.range();

    if (ret) {
      target = findMainTarget(unit, unit.x, unit.y, weaponRange, unit.type.targetAir, unit.type.targetGround);
    }

    noTargetTime += Time.delta;

    if (invalid(unit, target)) {
      if (target instanceof Healthc h && !h.isValid()) {
        targetInvalidated();
      }
      target = null;
    } else {
      noTargetTime = 0f;
    }

    // 非玩家控制时重置 isShooting；玩家控制时由 controlWeapons 管理
    if (!unit.isPlayer()) {
      unit.isShooting = false;
    }

    for (var mount : unit.mounts) {
      Weapon weapon = mount.weapon;
      float wrange = weapon.range();

      // let uncontrollable weapons do their own thing
      if (!weapon.controllable || weapon.noAttack)
        continue;

      if (!weapon.aiControllable) {
        mount.rotate = false;
        continue;
      }

      float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y),
          mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

      if (unit.type.singleTarget) {
        mount.target = target;
      } else {
        if (ret) {
          mount.target = findTarget(unit, mountX, mountY, wrange, weapon.bullet.collidesAir,
              weapon.bullet.collidesGround);
        }

        if (checkTarget(unit, mount.target, mountX, mountY, wrange)) {
          mount.target = null;
        }
      }

      boolean shoot = false;

      if (mount.target != null) {
        shoot = mount.target.within(mountX, mountY, wrange + (mount.target instanceof Sized s ? s.hitSize() / 2f : 0f))
            && shouldShoot();

        if (unit.type.autoDropBombs && !shoot) {
          if (bomberTarget == null || !bomberTarget.isAdded()
              || !bomberTarget.within(unit, unit.hitSize / 2f + ((Sized) bomberTarget).hitSize() / 2f)) {
            bomberTarget = Units.closestTarget(unit.team, unit.x, unit.y, unit.hitSize, u -> !u.isFlying(), t -> true);
          }
          shoot = bomberTarget != null;
        }

        Vec2 to = Predict.intercept(unit, mount.target, weapon.bullet);
        mount.aimX = to.x;
        mount.aimY = to.y;
      }

      mount.shoot = mount.rotate = shoot;
      if (!shouldFire()) {
        mount.shoot = false;
      }

      unit.isShooting |= mount.shoot;

      if (mount.target == null && !shoot && !Angles.within(mount.rotation, mount.weapon.baseRotation, 0.01f)
          && noTargetTime >= rotateBackTimer) {
        mount.rotate = true;
        Tmp.v1.trns(unit.rotation + mount.weapon.baseRotation, 5f);
        mount.aimX = mountX + Tmp.v1.x;
        mount.aimY = mountY + Tmp.v1.y;
      }

      if (shoot) {
        unit.aimX = mount.aimX;
        unit.aimY = mount.aimY;
      }
    }
  }

  public boolean shouldFire() {
    return true;
  }

  public boolean shouldShoot() {
    return true;
  }

  public boolean checkTarget(Unit unit, Teamc target, float x, float y, float range) {
    return Units.invalidateTarget(target, unit.team, x, y, range);
  }
}
