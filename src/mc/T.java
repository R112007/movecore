package mc;

import arc.graphics.Color;
import mc.abilities.AddWeaponAbility;
import mc.abilities.AddWeaponFieldAbility;
import mc.gen.RetractableLegsCoreUnit;
import mc.type.CoreUnitType;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.Sounds;
import mindustry.graphics.Pal;
import mindustry.type.Weapon;

public class T {
  public static CoreUnitType t;

  public static void ioad() {
    Weapon w = new Weapon("corvus-weapon") {
      {
        shootSound = Sounds.shootCorvus;
        chargeSound = Sounds.chargeCorvus;
        soundPitchMin = 1f;
        top = false;
        mirror = false;
        shake = 14f;
        shootY = 5f;
        x = y = 0;
        reload = 350f;
        recoil = 0f;

        cooldownTime = 350f;

        shootStatusDuration = 60f * 2f;
        shoot.firstShotDelay = Fx.greenLaserCharge.lifetime;
        parentizeEffects = true;

        bullet = new LaserBulletType() {
          {
            length = 460f;
            damage = 560f;
            width = 75f;

            lifetime = 65f;

            lightningSpacing = 35f;
            lightningLength = 5;
            lightningDelay = 1.1f;
            lightningLengthRand = 15;
            lightningDamage = 50;
            lightningAngleRand = 40f;
            largeHit = true;
            lightColor = lightningColor = Pal.heal;

            chargeEffect = Fx.greenLaserCharge;

            healPercent = 25f;
            collidesTeam = true;

            sideAngle = 15f;
            sideWidth = 0f;
            sideLength = 0f;
            colors = new Color[] { Pal.heal.cpy().a(0.4f), Pal.heal, Color.white };
          }
        };
      }
    };
    t = new CoreUnitType("t") {
      {
        this.hitSize = 30f;
        this.rotateSpeed = 3f;
        this.itemCapacity = 30;
        this.abilities.add(new AddWeaponFieldAbility(100, 180, 360, w));
        this.entity = "legs";
        this.constructor = RetractableLegsCoreUnit::create;
        this.canDrown = false;
        this.circleTarget = false;
        this.forceMultiTarget = true;
        this.buildSpeed = 0f;
        this.flying = true;
        this.speed = 1f;
        this.health = 3500;
        this.engineSize = 3.5f;
        this.engineOffset = 12f;
        this.lowAltitude = true;
        this.armor = 1;
      }
    };
  }
}
