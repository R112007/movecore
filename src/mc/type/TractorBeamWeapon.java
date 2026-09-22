package mc.type;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.audio.SoundLoop;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class TractorBeamWeapon extends Weapon {
  public float retargetTime = 5f;
  public float shootCone = 6f;
  public float shootLength = 5f;
  public float laserWidth = 0.6f;
  public float force = 0.3f;
  public float scaledForce = 0f;
  public float damage = 0f;
  public boolean targetAir = true, targetGround = true;
  public Color laserColor = Color.white;
  public StatusEffect status = StatusEffects.none;
  public float statusDuration = 300;
  public float range = 80f;

  public TextureRegion laser, laserStart, laserEnd;

  public TractorBeamWeapon(String name) {
    super(name);
  }

  public TractorBeamWeapon() {
  }

  {
    rotateSpeed = 10f;
    reload = 1f;
    predictTarget = false;
    autoTarget = false;
    controllable = false;
    rotate = true;
    mountType = TractorBeamMount::new;
    recoil = 0f;
    noAttack = true;
    useAttackRange = false;
    activeSound = Sounds.beamParallax;
    activeSoundVolume = 0.9f;
  }

  @Override
  public void addStats(UnitType u, Table t) {
    super.addStats(u, t);
    t.row();
    t.add("[lightgray]" + Stat.targetsAir.localized() + ": [white]" + targetAir);
    t.row();
    t.add("[lightgray]" + Stat.targetsGround.localized() + ": [white]" + targetGround);
    if (damage > 0) {
      t.row();
      t.add("[lightgray]" + Stat.damage.localized() + ": [white]" + (int) (damage * 60f) + " "
          + StatUnit.perSecond.localized());
    }
  }

  @Override
  public float range() {
    return range;
  }

  @Override
  public float dps() {
    return damage * 60f;
  }

  @Override
  public void load() {
    super.load();
    laser = Core.atlas.find("laser-white");
    laserStart = Core.atlas.find("laser-white-end");
    laserEnd = Core.atlas.find("laser-white-end");
  }

  @Override
  public void init() {
    super.init();
    bullet.range = range;
    bullet.collidesAir = targetAir;
    bullet.collidesGround = targetGround;
  }

  @Override
  public void update(Unit unit, WeaponMount mount) {
    TractorBeamMount t = (TractorBeamMount) mount;
    float weaponRotation = unit.rotation - 90,
        wx = unit.x + Angles.trnsx(weaponRotation, x, y),
        wy = unit.y + Angles.trnsy(weaponRotation, x, y);

    // 索敌
    if ((t.retarget -= Time.delta) <= 0f) {
      mount.target = Units.closestEnemy(unit.team, wx, wy, range(),
          u -> u.checkTarget(targetAir, targetGround));
      t.retarget = retargetTime;
    }

    // 验证目标
    if (mount.target != null && mount.target instanceof Unit u) {
      boolean invalid = !u.isAdded() || !u.isValid() || u.team() == unit.team
          || !u.within(wx, wy, range() + u.hitSize / 2f)
          || !u.checkTarget(targetAir, targetGround);

      if (invalid) {
        mount.target = null;
      }
    } else {
      mount.target = null;
    }

    t.any = false;

    if (mount.target != null && mount.target instanceof Unit targetUnit) {
      mount.aimX = targetUnit.x;
      mount.aimY = targetUnit.y;
      mount.shoot = true;
      mount.rotate = true;

      float targetRot = Angles.angle(wx, wy, targetUnit.x, targetUnit.y) - unit.rotation;
      mount.rotation = Angles.moveToward(mount.rotation, targetRot, rotateSpeed * Time.delta);

      t.lastX = targetUnit.x;
      t.lastY = targetUnit.y;

      float weaponRot = unit.rotation + mount.rotation;
      float dest = Angles.angle(wx, wy, targetUnit.x, targetUnit.y);
      boolean within = Angles.within(weaponRot, dest, shootCone);

      if (within) {
        float edelta = Time.delta;
        if (damage > 0) {
          targetUnit.damageContinuousPierce(damage * edelta * state.rules.unitDamage(unit.team));
        }
        if (status != StatusEffects.none) {
          targetUnit.apply(status, statusDuration);
        }
        t.any = true;
        targetUnit.impulseNet(Tmp.v1.set(wx, wy).sub(targetUnit)
            .limit((force + (1f - targetUnit.dst(wx, wy) / range()) * scaledForce) * edelta));
        t.strength = Mathf.lerpDelta(t.strength, 1f, 0.1f);

      } else {
        t.strength = Mathf.lerpDelta(t.strength, 0, 0.1f);
      }
    } else {
      mount.shoot = false;
      mount.rotate = false;
      t.strength = Mathf.lerpDelta(t.strength, 0, 0.1f);
    }
    if (!headless && activeSound != Sounds.none && t.any) {
      if (mount.sound == null)
        mount.sound = new SoundLoop(activeSound, activeSoundVolume);
      mount.sound.update(wx, wy, true);
    } else if (mount.sound != null) {
      mount.sound.update(wx, wy, false);
    }
  }

  @Override
  public void draw(Unit unit, WeaponMount mount) {
    super.draw(unit, mount);

    TractorBeamMount t = (TractorBeamMount) mount;

    // 视觉只在单位能射击时显示（被缴械/玩家未按键时不画，但伤害已打出）
    if (t.any && unit.canShoot()) {
      float z = Draw.z();
      Draw.z(Layer.bullet);
      float weaponRotation = unit.rotation - 90,
          wx = unit.x + Angles.trnsx(weaponRotation, x, y),
          wy = unit.y + Angles.trnsy(weaponRotation, x, y),
          ang = Angles.angle(wx, wy, t.lastX, t.lastY);

      Draw.mixcol(laserColor, Mathf.absin(4f, 0.6f));

      Drawf.laser(laser, laserStart, laserEnd,
          wx + Angles.trnsx(ang, shootLength), wy + Angles.trnsy(ang, shootLength),
          t.lastX, t.lastY, t.strength * laserWidth);

      Draw.mixcol();
      Draw.z(z);
    }
  }

  public static class TractorBeamMount extends WeaponMount {
    public float lastX, lastY, strength;
    public boolean any;
    public float retarget = 0f;

    public TractorBeamMount(Weapon weapon) {
      super(weapon);
    }
  }
}
