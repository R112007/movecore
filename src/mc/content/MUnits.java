package mc.content;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import mc.gen.MechCoreUnit;
import mc.gen.RetractableLegsCoreUnit;
import mc.type.CoreUnitType;
import mc.type.TractorBeamWeapon;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.Effect;
import mindustry.entities.Leg;
import mindustry.entities.abilities.EnergyFieldAbility;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.bullet.EmpBulletType;
import mindustry.entities.bullet.FlakBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.entities.effect.ExplosionEffect;
import mindustry.gen.Crawlc;
import mindustry.gen.Mechc;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.gen.Unitc;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Weapon;
import mindustry.type.weapons.MineWeapon;
import mindustry.type.weapons.PointDefenseWeapon;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;

public class MUnits {
  public static CoreUnitType moveCore1, mc2, mc3;

  public static void load() {
    mc3 = new CoreUnitType("mc3") {
      @Override
      public void drawMech(Mechc mech) {
        float legScale = 0.7f;
        Unit unit = (Unit) mech;

        Draw.reset();

        float e = unit.elevation;
        float sin = Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f / Mathf.PI, 1f), 0f, e);
        float extension = Mathf.lerp(mech.walkExtend(false), 0, e);
        float boostTrns = e * 2f;

        var floor = unit.isFlying() ? Blocks.air.asFloor() : unit.floorOn();

        if (floor.isLiquid) {
          Draw.color(Color.white, floor.mapColor, 0.5f);
        }

        for (int i : Mathf.signs) {
          Draw.mixcol(Tmp.c1.set(mechLegColor).lerp(Color.white, Mathf.clamp(unit.hitTime)),
              Math.max(Math.max(0, i * extension / mechStride), unit.hitTime));

          // 关键：width 和 height 都乘以 legScale（0.7f）
          Draw.rect(legRegion,
              unit.x + Angles.trnsx(mech.baseRotation(), extension * i - boostTrns, -boostTrns * i),
              unit.y + Angles.trnsy(mech.baseRotation(), extension * i - boostTrns, -boostTrns * i),
              legRegion.width * legRegion.scl() * i * legScale,
              legRegion.height * legRegion.scl() * (1 - Math.max(-sin * i, 0) * 0.5f) * legScale,
              mech.baseRotation() - 90 + 35f * i * e);
        }

        Draw.mixcol(Color.white, unit.hitTime);

        if (unit.lastDrownFloor != null) {
          Draw.color(Color.white, Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.83f), unit.drownTime * 0.9f);
        } else {
          Draw.color(Color.white);
        }

        Draw.rect(baseRegion, unit, mech.baseRotation() - 90);
        Draw.mixcol();
      }

      {
        boostMultiplier = 3.5f;
        canBoost = true;
        engineSize = 0;
        engineOffset = 0f;
        outlineColor = Color.valueOf("#34343BFF");
        hitSize = 32;
        health = 9000;
        useStringEntity = false;
        engines.add(new UnitEngine(4.25f, -12, 4, 0));
        engines.add(new UnitEngine(-4.25f, -12, 4, 0));
        unitCapBonus = 6;
        mineSpeed = 0.8f;
        mineTier = 2;
        buildSpeed = 1f;
        storageCapacity = 5000;
        controller = UnitTypes.dagger.controller;
        constructor = MechCoreUnit::create;
        speed = 0.5f;
        rotateSpeed = 1.2f;
        ArtilleryBulletType fragBullet1 = new ArtilleryBulletType() {
          {
            shrinkY = 0f;
            status = StatusEffects.burning; // 状态效果被注释掉了
            smokeEffect = Fx.none;
            trailEffect = Fx.none;
            lifetime = 10f;
            speed = 2f;
            trailChance = 0f;
            width = 18f * 0.7f;
            height = 18f * 0.7f;

            damage = 40f;
            splashDamage = 20f;
            splashDamageRadius = 30f;
            shootEffect = Fx.bigShockwave;
            hitEffect = Fx.massiveExplosion;
            backColor = Color.valueOf("ffffff");
            frontColor = Pal.heal;
            collidesGround = true;
            collidesAir = true;
            buildingDamageMultiplier = 1f;
          }
        };
        weapons.add(new Weapon("mcore-mc31") {
          {
            x = -13;
            y = 2.75f;
            rotate = false;
            recoil = 5;
            mirror = false;
            shake = 4f;
            shootSound = Sounds.shootRipple;
            reload = 110;
            bullet = new ArtilleryBulletType() {
              {
                // 基础属性
                shrinkY = 0f;
                status = StatusEffects.electrified;
                smokeEffect = Fx.none;
                trailEffect = Fx.none;
                lifetime = 40f;
                speed = 2f;
                trailChance = 0f;
                width = 14f;
                height = 14f;

                damage = 60f;
                splashDamage = 40f;
                splashDamageRadius = 30f;
                shootEffect = Fx.bigShockwave;
                hitEffect = Fx.massiveExplosion;
                backColor = Color.valueOf("ffffff");
                frontColor = Pal.heal;
                collidesGround = true;
                collidesAir = true;
                lightningDamage = 20f;
                lightning = 2;
                lightningColor = Pal.heal;
                lightningLength = 12;
                buildingDamageMultiplier = 1f;
                fragLifeMin = 0.3f;
                fragBullets = 10;
                fragBullet = fragBullet1; // 关联子炮弹
              }
            };

          }
        });
        weapons.add(new TractorBeamWeapon("mcore-mc32") {
          {
            x = 14.25f;
            y = 1;
            range = 120f;
            force = 35f;
            mirror = false;
            scaledForce = 1f;
            damage = 3f;
            shootLength = 6f;
            laserColor = Pal.heal;
            rotate = true;
            autoTarget = true;
          }
        });
      }
    };
    mc2 = new CoreUnitType("mc2") {
      @Override
      public <T extends Unit & mindustry.gen.Legsc> void drawLegs(T unit) {
        // 定义缩放系数
        float scl = 0.8f * (1 - 0.125f);

        // 以下是原始 drawLegs 方法的逻辑，但所有尺寸都应用了缩放系数 'scl'
        applyColor(unit);
        Vec2 legOffset = new Vec2(); // 假设这是类中的一个临时变量，这里为了完整性重新声明
        Vec2 Tmp_v1 = new Vec2();

        Leg[] legs = unit.legs();
        // 原始代码中的 ssize 是 footRegion 的宽度，现在乘以 scl
        float ssize = footRegion.width * footRegion.scl() * 1.5f * scl;
        float rotation = unit.baseRotation();
        float invDrown = 1f - unit.drownTime;

        if (footRegion.found()) {
          for (Leg leg : legs) {
            Drawf.shadow(leg.base.x, leg.base.y, ssize, invDrown);
          }
        }

        // legs are drawn front first
        for (int j = legs.length - 1; j >= 0; j--) {
          int i = (j % 2 == 0 ? j / 2 : legs.length - 1 - j / 2);
          Leg leg = legs[i];
          boolean flip = i >= legs.length / 2f;
          int flips = Mathf.sign(flip);
          Vec2 position = unit.legOffset(legOffset, i).add(unit);

          Tmp_v1.set(leg.base).sub(leg.joint).inv().setLength(legExtension);

          if (footRegion.found() && leg.moving && shadowElevation > 0) {
            float sclShadow = shadowElevation * invDrown;
            float elev = Mathf.slope(1f - leg.stage) * sclShadow;
            Draw.color(Pal.shadow);
            Draw.rect(footRegion, leg.base.x + shadowTX * elev, leg.base.y + shadowTY * elev,
                position.angleTo(leg.base));
            Draw.color();
          }

          Draw.mixcol(Draw.getMixColor(), Draw.getMixColor().a);

          if (footRegion.found()) {
            Draw.rect(footRegion, leg.base.x, leg.base.y, position.angleTo(leg.base));
          }

          if (legBaseUnder) {
            // legBaseRegion 的高度乘以 scl
            Lines.stroke(legBaseRegion.height * legRegion.scl() * flips * scl);
            Lines.line(legBaseRegion, leg.joint.x + Tmp_v1.x, leg.joint.y + Tmp_v1.y, leg.base.x, leg.base.y, false);
            // legRegion 的高度乘以 scl
            Lines.stroke(legRegion.height * legRegion.scl() * flips * scl);
            Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, false);
          } else {
            // legRegion 的高度乘以 scl
            Lines.stroke(legRegion.height * legRegion.scl() * flips * scl);
            Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, false);
            // legBaseRegion 的高度乘以 scl
            Lines.stroke(legBaseRegion.height * legRegion.scl() * flips * scl);
            Lines.line(legBaseRegion, leg.joint.x + Tmp_v1.x, leg.joint.y + Tmp_v1.y, leg.base.x, leg.base.y, false);
          }

          if (jointRegion.found()) {
            Draw.rect(jointRegion, leg.joint.x, leg.joint.y);
          }
        }

        // base joints are drawn after everything else
        if (baseJointRegion.found()) {
          for (int j = legs.length - 1; j >= 0; j--) {
            Vec2 position = unit.legOffset(legOffset, (j % 2 == 0 ? j / 2 : legs.length - 1 - j / 2)).add(unit);
            Draw.rect(baseJointRegion, position.x, position.y, rotation);
          }
        }

        if (baseRegion.found()) {
          Draw.rect(baseRegion, unit.x, unit.y, rotation - 90);
        }
        Draw.reset();
      }

      {
        float scl = 0.8f * (1 - 0.125f);
        hitSize = 50;
        health = 14000;
        useStringEntity = false;
        unitCapBonus = 10;
        mineSpeed = 1f;
        outlineColor = Color.valueOf("#34343BFF");
        mineTier = 2;
        buildSpeed = 1f;
        storageCapacity = 5000;
        controller = UnitTypes.poly.controller;
        constructor = RetractableLegsCoreUnit::create;
        speed = 0.5f;
        rotateSpeed = 0.7f;
        // ========== 尺寸 ==========
        hitSize = 14f;
        legCount = 4;
        legLength = 14f;
        legBaseOffset = 11f;
        legMoveSpace = 1.5f;
        legForwardScl = 0.58f;
        legSplashDamage = 80;
        legSplashRange = 60;
        // outlineColor = Color.valueOf("#43434FFF");
        weapons.add(new Weapon("mcore-mc21") {
          {
            x = 16.5f;
            y = 3;
            rotate = false;
            mirror = false;
            shake = 4f;
            reload = 156;
            shoot.firstShotDelay = Fx.greenLaserChargeSmall.lifetime - 1f;
            recoil = 0f;
            chargeSound = Sounds.chargeVela;
            shootSound = Sounds.beamPlasma;
            initialShootSound = Sounds.shootBeamPlasma;
            cooldownTime = 200f;
            continuous = true;
            bullet = new ContinuousLaserBulletType() {
              {
                damage = 35f;
                length = 150f;
                hitEffect = Fx.hitMeltHeal;
                drawSize = 420f;
                lifetime = 160f;
                shake = 1f;
                despawnEffect = Fx.smokeCloud;
                smokeEffect = Fx.none;
                width = 4f;
                chargeEffect = Fx.greenLaserChargeSmall;

                incendChance = 0.1f;
                incendSpread = 5f;
                incendAmount = 1;

                // constant healing
                healPercent = 1f;
                collidesTeam = true;

                colors = new Color[] { Pal.heal.cpy().a(.2f), Pal.heal.cpy().a(.5f), Pal.heal.cpy().mul(1.2f),
                    Color.white };
              }
            };
          }
        });
        weapons.add(new Weapon("mcore-mc21") {
          {
            x = -16.5f;
            y = 3;
            rotate = false;
            mirror = false;
            reload = 55f;
            shootSound = Sounds.shootLancer;
            bullet = new LaserBulletType() {
              {
                width = 25f;
                damage = 85f;
                recoil = 0f;
                sideAngle = 45f;
                sideWidth = 1f;
                sideLength = 70f;
                healPercent = 10f;
                collidesTeam = true;
                length = 150f;
                colors = new Color[] { Pal.heal.cpy().a(0.4f), Pal.heal, Color.white };
              }
            };
          }
        });
        weapons.add(new Weapon("mcore-mc22") {
          {
            reload = 240f;
            x = 15.5f;
            y = -7f;
            mirror = true;
            shadow = 5f;
            top = true;
            rotateSpeed = 4f;
            rotate = true;
            inaccuracy = 1f;
            velocityRnd = 0.1f;
            shootSound = Sounds.explosionQuad;
            ejectEffect = Fx.none;
            bullet = new FlakBulletType(2.5f, 90) {
              {
                sprite = "missile-large";
                collidesGround = collidesAir = true;
                explodeRange = 40f;
                width = height = 12f;
                shrinkY = 0f;
                drag = -0.003f;
                homingRange = 60f;
                keepVelocity = false;
                lightRadius = 60f;
                lightOpacity = 0.7f;
                lightColor = Pal.heal;
                despawnSound = Sounds.explosion;

                splashDamageRadius = 30f;
                splashDamage = 128f;

                lifetime = 80f;
                backColor = Pal.heal;
                frontColor = Color.white;

                hitEffect = MFx.energyParticleField;

                weaveScale = 8f;
                weaveMag = 1f;

                trailColor = Pal.heal;
                trailWidth = 4.5f;
                trailLength = 29;

                fragBullets = 7;
                fragVelocityMin = 0.3f;

                fragBullet = new MissileBulletType(3.9f, 80) {
                  {
                    homingPower = 0.2f;
                    weaveMag = 4;
                    weaveScale = 4;
                    lifetime = 60f;
                    keepVelocity = false;
                    shootEffect = Fx.shootHeal;
                    smokeEffect = Fx.hitLaser;
                    splashDamage = 80f;
                    splashDamageRadius = 20f;
                    frontColor = Color.white;
                    hitSound = Sounds.none;

                    lightColor = Pal.heal;
                    lightRadius = 40f;
                    lightOpacity = 0.7f;

                    trailColor = Pal.heal;
                    trailWidth = 2.5f;
                    trailLength = 20;
                    trailChance = -1f;

                    healPercent = 2.8f;
                    collidesTeam = true;
                    backColor = Pal.heal;

                    despawnEffect = Fx.none;
                    hitEffect = new ExplosionEffect() {
                      {
                        lifetime = 20f;
                        waveStroke = 2f;
                        waveColor = Pal.heal;
                        waveRad = 12f;
                        smokeSize = 0f;
                        smokeSizeBase = 0f;
                        sparkColor = Pal.heal;
                        sparks = 9;
                        sparkRad = 35f;
                        sparkLen = 4f;
                        sparkStroke = 1.5f;
                      }
                    };
                  }
                };
              }
            };
          }
        });
      }
    };
    moveCore1 = new CoreUnitType("moveCore1") {
      {
        health = 22000;
        hitSize = 60;
        useStringEntity = false;
        unitCapBonus = 16;
        outlineColor = Color.valueOf("#34343BFF");
        mineSpeed = 2;
        mineTier = 3;
        buildSpeed = 2;
        storageCapacity = 7000;
        controller = UnitTypes.poly.controller;
        constructor = RetractableLegsCoreUnit::create;
        speed = 0.5f;
        rotateSpeed = 0.7f;
        legCount = 6;
        legGroupSize = 2;
        legLength = 30;
        legBaseOffset = 6;
        legSpeed = 0.06f;
        legMoveSpace = 1f;
        legForwardScl = 0.7f;
        legMinLength = 0.7f;
        legMaxLength = 1.2f;
        legSplashDamage = 8;
        legSplashRange = 10;
        legBaseUnder = true;
        stepShake = 3;
        legSplashDamage = 80;
        legSplashRange = 60;
        hitSize = 40;
        weapons.add(new Weapon("mcore-moveCore1-w1") {
          {
            x = -22.75f;
            y = 1.5f;
            rotate = false;
            reload = 65f;
            shake = 3f;
            recoil = 2f;
            cooldownTime = reload - 10f;
            shootSound = Sounds.shootNavanax;
            bullet = new EmpBulletType() {
              {
                float rad = 100f;
                scaleLife = true;
                lightOpacity = 0.7f;
                unitDamageScl = 0.8f;
                healPercent = 10f;
                timeIncrease = 3f;
                timeDuration = 60f * 20f;
                powerDamageScl = 3f;
                damage = 260;
                hitColor = lightColor = Pal.heal;
                lightRadius = 50f;
                clipSize = 160f;
                shootEffect = Fx.hitEmpSpark;
                smokeEffect = Fx.shootBigSmoke2;
                lifetime = 60f;
                sprite = "circle-bullet";
                backColor = Pal.heal;
                frontColor = Color.white;
                width = height = 12f;
                shrinkY = 0f;
                speed = 5f;
                trailLength = 20;
                trailWidth = 6f;
                trailColor = Pal.heal;
                trailInterval = 3f;
                splashDamage = 220f;
                splashDamageRadius = rad;
                hitShake = 4f;
                trailRotation = true;
                status = StatusEffects.electrified;
                hitSound = Sounds.explosionNavanax;

                trailEffect = new Effect(16f, e -> {
                  color(Pal.heal);
                  for (int s : Mathf.signs) {
                    Drawf.tri(e.x, e.y, 4f, 30f * e.fslope(), e.rotation + 90f * s);
                  }
                });

                hitEffect = new Effect(50f, 100f, e -> {
                  e.scaled(7f, b -> {
                    color(Pal.heal, b.fout());
                    Fill.circle(e.x, e.y, rad);
                  });

                  color(Pal.heal);
                  stroke(e.fout() * 3f);
                  Lines.circle(e.x, e.y, rad);

                  int points = 10;
                  float offset = Mathf.randomSeed(e.id, 360f);
                  for (int i = 0; i < points; i++) {
                    float angle = i * 360f / points + offset;
                    Drawf.tri(e.x + Angles.trnsx(angle, rad), e.y + Angles.trnsy(angle, rad), 6f, 50f * e.fout(),
                        angle);
                  }

                  Fill.circle(e.x, e.y, 12f * e.fout());
                  color();
                  Fill.circle(e.x, e.y, 6f * e.fout());
                  Drawf.light(e.x, e.y, rad * 1.6f, Pal.heal, e.fout());
                });
              }
            };
          }
        });
        weapons.add(new MineWeapon("mcore-moveCore1-w2") {
          {
            x = 13.75f;
            y = 15.75f;
            shootY = 6;
          }
        });
        weapons.add(new PointDefenseWeapon("mcore-moveCore1-w2") {
          {
            x = 18.25f;
            y = -10.25f;
            reload = 4f;
            targetInterval = 8f;
            targetSwitchInterval = 8f;
            bullet = new BulletType() {
              {
                shootEffect = Fx.sparkShoot;
                hitEffect = Fx.pointHit;
                maxRange = 180f;
                damage = 30f;
              }
            };
          }
        });
        abilities.add(new EnergyFieldAbility(60f, 120f, 200f) {
          {
            y = 16.5f;
            layer = Layer.groundUnit - 0.01f;
            effectRadius = 3f;
          }
        });
      }
    };
  }
}
