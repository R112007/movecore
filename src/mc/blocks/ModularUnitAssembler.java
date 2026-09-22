package mc.blocks;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.EnumSet;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public class ModularUnitAssembler extends PayloadBlock {
  public TextureRegion sideRegion1;
  public TextureRegion sideRegion2;

  public int areaSize = 11;
  public UnitType droneType = UnitTypes.assemblyDrone;
  public int dronesCreated = 4;
  public float droneConstructTime = 60f * 4f;
  public int[] capacities = {};
  public Sound createSound = Sounds.unitCreateBig;
  public float createSoundVolume = 1f;

  public Seq<AssemblerUnitPlan> plans = new Seq<>();

  public boolean ignoreUnitBan = true;

  protected @Nullable ConsumePayloadDynamic consPayload;
  protected @Nullable ConsumeItemDynamic consItem;

  public ModularUnitAssembler(String name) {
    super(name);
    update = solid = true;
    rotate = true;
    rotateDraw = false;
    acceptsPayload = hasItems = true;
    flags = EnumSet.of(BlockFlag.unitAssembler);
    regionRotated1 = 1;
    sync = true;
    group = BlockGroup.units;
    commandable = true;
    quickRotate = false;
    ambientSound = Sounds.loopUnitBuilding;
    ambientSoundVolume = 0.13f;
    configurable = true;
    clearOnDoubleTap = true;

    config(Integer.class, (ModularUnitAssemblerBuild build, Integer i) -> {
      if (!configurable)
        return;
      if (build.currentPlan == i)
        return;
      build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
      build.progress = 0f;
      if (build.command != null && (build.unit() == null || !build.unit().commands.contains(build.command))) {
        build.command = null;
      }
    });

    config(UnitType.class, (ModularUnitAssemblerBuild build, UnitType val) -> {
      if (!configurable)
        return;
      int next = plans.indexOf(p -> p.unit == val);
      if (build.currentPlan == next)
        return;
      build.currentPlan = next;
      build.progress = 0f;
      if (build.command != null && !val.commands.contains(build.command)) {
        build.command = null;
      }
    });

    config(UnitCommand.class, (ModularUnitAssemblerBuild build, UnitCommand command) -> build.command = command);

    configClear((ModularUnitAssemblerBuild build) -> {
      build.currentPlan = -1;
      build.progress = 0f;
      build.command = null;
    });
  }

  @Override
  public void load() {
    super.load();
    sideRegion1 = Core.atlas.find(name + "-side1");
    sideRegion2 = Core.atlas.find(name + "-side2");
  }

  public Rect getRect(Rect rect, float x, float y, int rotation) {
    rect.setCentered(x, y, areaSize * tilesize);
    float len = tilesize * (areaSize + size) / 2f;
    rect.x += Geometry.d4x(rotation) * len;
    rect.y += Geometry.d4y(rotation) * len;
    return rect;
  }

  @Override
  public void drawPlace(int x, int y, int rotation, boolean valid) {
    super.drawPlace(x, y, rotation, valid);
    x *= tilesize;
    y *= tilesize;
    x += offset;
    y += offset;
    Rect rect = getRect(Tmp.r1, x, y, rotation);
    Drawf.dashRect(valid ? Pal.accent : Pal.remove, rect);
  }

  @Override
  public boolean canPlaceOn(Tile tile, Team team, int rotation) {
    Rect rect = getRect(Tmp.r1, tile.worldx() + offset, tile.worldy() + offset, rotation).grow(0.1f);
    return !indexer.getFlagged(team, BlockFlag.unitAssembler)
        .contains(b -> b != tile.build && b.block instanceof ModularUnitAssembler assembler
            && assembler.getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect))
        && !team.data().getBuildings(ConstructBlock.get(size))
            .contains(b -> b != tile.build && ((ConstructBuild) b).current instanceof ModularUnitAssembler assembler
                && assembler.getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect));
  }

  @Override
  public void setBars() {
    super.setBars();

    boolean planLiquids = false;
    for (int i = 0; i < plans.size; i++) {
      var req = plans.get(i).liquidReq;
      if (req != null && req.length > 0) {
        for (var stack : req) {
          addLiquidBar(stack.liquid);
        }
        planLiquids = true;
      }
    }

    if (planLiquids) {
      removeBar("liquid");
    }

    addBar("progress", (ModularUnitAssemblerBuild e) -> new Bar("bar.progress", Pal.ammo, () -> e.fraction()));

    addBar("units", (ModularUnitAssemblerBuild e) -> {
      var plan = e.getPlan();
      return plan == null ? new Bar(() -> "...", () -> Pal.power, () -> 0f)
          : new Bar(() -> Core.bundle.format("bar.unitcap",
              Fonts.getUnicodeStr(e.unit().name),
              e.team.data().countType(e.unit()),
              plan.maxUnits),
              () -> Pal.power,
              () -> Math.min((float) e.team.data().countType(e.unit()) / plan.maxUnits, 1f));
    });
  }

  @Override
  public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
    Draw.rect(region, plan.drawx(), plan.drawy());
    Draw.rect(plan.rotation >= 2 ? sideRegion2 : sideRegion1, plan.drawx(), plan.drawy(), plan.rotation * 90);
    Draw.rect(topRegion, plan.drawx(), plan.drawy());
  }

  @Override
  public TextureRegion[] icons() {
    return new TextureRegion[] { region, sideRegion1, topRegion };
  }

  @Override
  public void init() {
    updateClipRadius((areaSize + 1) * tilesize);

    consume(consPayload = new ConsumePayloadDynamic((ModularUnitAssemblerBuild build) -> {
      var plan = build.getPlan();
      return plan != null && plan.requirements != null ? plan.requirements : new Seq<>();
    }));
    consume(consItem = new ConsumeItemDynamic((ModularUnitAssemblerBuild build) -> {
      var plan = build.getPlan();
      return plan != null && plan.itemReq != null ? plan.itemReq : ItemStack.empty;
    }));
    consume(new ConsumeLiquidsDynamic((ModularUnitAssemblerBuild build) -> {
      var plan = build.getPlan();
      return plan != null && plan.liquidReq != null ? plan.liquidReq : LiquidStack.empty;
    }));

    super.init();
    initCapacities();
  }

  @Override
  public void afterPatch() {
    super.afterPatch();
    initCapacities();
  }

  public void initCapacities() {
    consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));

    itemCapacity = 10;
    capacities = new int[Vars.content.items().size];
    for (AssemblerUnitPlan plan : plans) {
      if (plan == null || plan.unit == null)
        continue;
      if (plan.itemReq != null) {
        for (ItemStack stack : plan.itemReq) {
          capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
          itemCapacity = Math.max(itemCapacity, stack.amount * 2);
        }
      }
      if (plan.liquidReq != null) {
        for (LiquidStack stack : plan.liquidReq) {
          liquidFilter[stack.liquid.id] = true;
        }
      }
    }
  }

  @Override
  public void checkContentArrayCapacity(int items, int liquids) {
    super.checkContentArrayCapacity(items, liquids);
    if (capacities.length != items)
      capacities = Arrays.copyOf(capacities, items);
  }

  @Override
  public void setStats() {
    super.setStats();

    stats.add(Stat.output, table -> {
      table.row();
      for (var plan : plans) {
        if (plan == null || plan.unit == null)
          continue;
        table.table(Styles.grayPanel, t -> {
          if (plan.unit != null && plan.unit.isBanned() && !ignoreUnitBan) {
            t.image(Icon.cancel).color(Pal.remove).size(40).pad(10);
            return;
          }

          if (plan.unit != null && plan.unit.unlockedNow()) {
            t.image(plan.unit.uiIcon).scaling(Scaling.fit).size(40).pad(10f).left()
                .with(i -> StatValues.withTooltip(i, plan.unit));
            t.table(info -> {
              info.defaults().left();
              info.add(plan.unit.localizedName);
              info.row();
              info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds"))
                  .color(Color.lightGray);
              info.row();
              info.add("Max: " + plan.maxUnits).color(Color.lightGray);
              if (ignoreUnitBan) {
                info.row();
                info.add("[accent]Ignore Ban").color(Pal.accent);
              }
            }).left();

            t.table(req -> {
              req.add().grow();
              req.table(solid -> {
                int length = 0;
                if (plan.itemReq != null) {
                  for (int i = 0; i < plan.itemReq.length; i++) {
                    if (length % 6 == 0)
                      solid.row();
                    solid.add(StatValues.stack(plan.itemReq[i])).pad(5);
                    length++;
                  }
                }
                for (int i = 0; i < plan.requirements.size; i++) {
                  if (length % 6 == 0)
                    solid.row();
                  solid.add(StatValues.stack(plan.requirements.get(i))).pad(5);
                  length++;
                }
              }).right();

              LiquidStack[] stacks = plan.liquidReq;
              if (stacks != null) {
                for (int i = 0; i < plan.liquidReq.length; i++) {
                  req.row();
                  req.add().grow();
                  req.add(StatValues.displayLiquid(stacks[i].liquid, stacks[i].amount * 60f, true)).right();
                }
              }
            }).grow().pad(10f);
          } else {
            t.image(Icon.lock).color(Pal.darkerGray).size(40).pad(10);
          }
        }).growX().pad(5);
        table.row();
      }
    });
  }

  public static class AssemblerUnitPlan {
    public UnitType unit = UnitTypes.dagger;
    public @Nullable Seq<PayloadStack> requirements;
    public @Nullable ItemStack[] itemReq;
    public @Nullable LiquidStack[] liquidReq;
    public float time;
    public int maxUnits;

    public AssemblerUnitPlan(UnitType unit, float time, int maxUnits, Seq<PayloadStack> requirements) {
      this.unit = unit;
      this.time = time;
      this.maxUnits = maxUnits;
      this.requirements = requirements;
    }

    public AssemblerUnitPlan() {
    }
  }

  public class ModularUnitAssemblerBuild extends PayloadBlockBuild<Payload> {
    protected IntSeq readUnits = new IntSeq();
    protected IntSeq whenSyncedUnits = new IntSeq();

    public @Nullable Vec2 commandPos;
    public @Nullable UnitCommand command;
    public Seq<Unit> units = new Seq<>();
    public PayloadSeq blocks = new PayloadSeq();
    public float progress, warmup, droneWarmup, powerWarmup, sameTypeWarmup;
    public float invalidWarmup = 0f;
    public boolean wasOccupied = false;
    public float droneProgress, totalDroneProgress;
    public int currentPlan = -1;

    public AssemblerUnitPlan getPlan() {
      return currentPlan < 0 || currentPlan >= plans.size ? null : plans.get(currentPlan);
    }

    public UnitType unit() {
      var plan = getPlan();
      return plan == null ? UnitTypes.alpha : plan.unit;
    }

    public boolean isAtUnitCap() {
      var plan = getPlan();
      if (plan == null)
        return true;
      return team.data().countType(plan.unit) >= plan.maxUnits;
    }

    public boolean isUnitBanned() {
      var plan = getPlan();
      if (plan == null)
        return true;
      if (ignoreUnitBan)
        return false;
      return plan.unit.isBanned();
    }

    public float fraction() {
      return progress;
    }

    public Vec2 getUnitSpawn() {
      float len = tilesize * (areaSize + size) / 2f;
      float unitX = x + Geometry.d4x(rotation) * len, unitY = y + Geometry.d4y(rotation) * len;
      return Tmp.v4.set(unitX, unitY);
    }

    public boolean checkSolid(Vec2 v, boolean same) {
      var output = unit();
      float hsize = output.hitSize * 1.4f;
      return ((!output.flying
          && collisions.overlapsTile(Tmp.r1.setCentered(v.x, v.y, output.hitSize), EntityCollisions::solid)) ||
          Units.anyEntities(v.x - hsize / 2f, v.y - hsize / 2f, hsize, hsize,
              u -> (!same || u.type != output) && !u.spawnedByCore &&
                  ((u.type.allowLegStep && output.allowLegStep) || (output.flying && u.isFlying())
                      || (!output.flying && u.isGrounded()))));
    }

    @Override
    public boolean shouldConsume() {
      var plan = getPlan();
      return enabled && !wasOccupied && plan != null && !isAtUnitCap() && !isUnitBanned()
          && consPayload.efficiency(this) > 0 && consItem.efficiency(this) > 0
          && team.activateUnitFactories();
    }

    @Override
    public void drawSelect() {
      super.drawSelect();
      if (plans.size > 1 && currentPlan != -1 && currentPlan < plans.size) {
        drawItemSelection(plans.get(currentPlan).unit);
      }
      Drawf.dashRect(Tmp.c1.set(Pal.accent).lerp(Pal.remove, invalidWarmup), getRect(Tmp.r1, x, y, rotation));
    }

    @Override
    public void display(Table table) {
      super.display(table);
      if (team != player.team())
        return;

      table.row();
      table.table(t -> {
        t.left().defaults().left();
        t.label(() -> "[accent] -> []" + unit().emoji() + " " + unit().localizedName);
      }).pad(4).padLeft(0f).fillX().left();
    }

    @Override
    public void created() {
      if (currentPlan == -1) {
        currentPlan = plans.indexOf(p -> p != null && p.unit != null && (!p.unit.isBanned() || ignoreUnitBan));
      }
    }

    @Override
    public void updateTile() {
      if (!readUnits.isEmpty()) {
        units.clear();
        readUnits.each(i -> {
          var unit = Groups.unit.getByID(i);
          if (unit != null)
            units.add(unit);
        });
        readUnits.clear();
      }

      if (units.size < dronesCreated && whenSyncedUnits.size > 0) {
        whenSyncedUnits.each(id -> {
          var unit = Groups.unit.getByID(id);
          if (unit != null)
            units.addUnique(unit);
        });
      }

      units.removeAll(u -> !u.isAdded() || u.dead || !(u.controller() instanceof AssemblerAI));

      if (!allowUpdate()) {
        progress = 0f;
        units.each(Unit::kill);
        units.clear();
      }

      float powerStatus = !enabled ? 0f : power == null ? 1f : power.status;
      powerWarmup = Mathf.lerpDelta(powerStatus, powerStatus > 0.0001f ? 1f : 0f, 0.1f);
      droneWarmup = Mathf.lerpDelta(droneWarmup, units.size < dronesCreated ? powerStatus : 0f, 0.1f);
      totalDroneProgress += droneWarmup * delta();

      // 一只一只造，造完一只重置进度
      if (units.size < dronesCreated && enabled
          && (droneProgress += delta() * state.rules.unitBuildSpeed(team) * powerStatus / droneConstructTime) >= 1f) {
        if (!net.client()) {
          var unit = droneType.create(team);
          if (unit.controller() instanceof AssemblerAI) {
            if (unit instanceof BuildingTetherc bt) {
              bt.building(this);
            }
            unit.set(x, y);
            unit.rotation = 90f;
            unit.add();
            units.add(unit);
            droneProgress = 0f; // ← 关键修复：生成一只就重置
          } else {
            droneProgress = 0f;
          }
        }
      }

      if (units.size >= dronesCreated) {
        droneProgress = 0f;
      }

      Vec2 spawn = getUnitSpawn();

      if (moveInPayload() && !wasOccupied) {
        yeetPayload(payload);
        payload = null;
      }

      for (int i = 0; i < units.size; i++) {
        var unit = units.get(i);
        var ai = (AssemblerAI) unit.controller();
        ai.targetPos.trns(i * 90f + 45f, areaSize / 2f * Mathf.sqrt2 * tilesize).add(spawn);
        ai.targetAngle = i * 90f + 45f + 180f;
      }

      wasOccupied = checkSolid(spawn, false);
      boolean visualOccupied = checkSolid(spawn, true);
      float eff = (units.count(u -> ((AssemblerAI) u.controller()).inPosition()) / (float) dronesCreated);

      sameTypeWarmup = Mathf.lerpDelta(sameTypeWarmup, wasOccupied && !visualOccupied ? 0f : 1f, 0.1f);
      invalidWarmup = Mathf.lerpDelta(invalidWarmup, visualOccupied ? 1f : 0f, 0.1f);

      var plan = getPlan();

      if (plan == null || isAtUnitCap() || isUnitBanned()) {
        warmup = Mathf.lerpDelta(warmup, 0f, 0.1f);
        return;
      }

      if (!wasOccupied && efficiency > 0) {
        warmup = Mathf.lerpDelta(warmup, efficiency, 0.1f);

        if ((progress += edelta() * state.rules.unitBuildSpeed(team) * eff / plan.time) >= 1f) {
          if (!net.client()) {
            spawned();
          }
        }
      } else {
        warmup = Mathf.lerpDelta(warmup, 0f, 0.1f);
      }
    }

    public void spawned() {
      var plan = getPlan();
      if (plan == null)
        return;

      Vec2 spawn = getUnitSpawn();
      consume();

      var unit = plan.unit.create(team);
      if (unit.isCommandable()) {
        if (commandPos != null) {
          unit.command().commandPosition(commandPos);
        }
        unit.command()
            .command(command == null && unit.type.defaultCommand != null ? unit.type.defaultCommand : command);
      }
      unit.set(spawn.x + Mathf.range(0.001f), spawn.y + Mathf.range(0.001f));
      unit.rotation = rotdeg();
      var targetBuild = unit.buildOn();
      var payload = new UnitPayload(unit);
      if (targetBuild != null && targetBuild.team == team && targetBuild.acceptPayload(targetBuild, payload)) {
        targetBuild.handlePayload(targetBuild, payload);
      } else if (!net.client()) {
        unit.add();
        Units.notifyUnitSpawn(unit);
      }

      createSound.at(spawn.x, spawn.y, 1f + Mathf.range(0.06f), createSoundVolume);

      progress = 0f;
      Fx.unitAssemble.at(spawn.x, spawn.y, rotdeg() - 90f, plan.unit);
      blocks.clear();

      Events.fire(new UnitCreateEvent(unit, this));
    }

    @Override
    public void draw() {
      Draw.rect(region, x, y);

      for (int i = 0; i < 4; i++) {
        if (blends(i) && i != rotation) {
          Draw.rect(inRegion, x, y, (i * 90) - 180);
        }
      }

      Draw.rect(rotation >= 2 ? sideRegion2 : sideRegion1, x, y, rotdeg());

      Draw.z(Layer.blockOver);

      payRotation = rotdeg();
      drawPayload();

      Draw.z(Layer.blockOver + 0.1f);

      Draw.rect(topRegion, x, y);

      if (isPayload())
        return;

      if (droneWarmup > 0.001f) {
        Draw.draw(Layer.blockOver + 0.2f, () -> {
          Drawf.construct(this, droneType.fullIcon, Pal.accent, 0f, droneProgress, droneWarmup, totalDroneProgress,
              14f);
        });
      }

      Vec2 spawn = getUnitSpawn();
      float sx = spawn.x, sy = spawn.y;

      var plan = getPlan();
      if (plan == null)
        return;

      Draw.draw(Layer.blockBuilding, () -> {
        Draw.color(Pal.accent, warmup);
        Shaders.blockbuild.region = plan.unit.fullIcon;
        Shaders.blockbuild.time = Time.time;
        Shaders.blockbuild.alpha = warmup;
        Shaders.blockbuild.progress = Mathf.clamp(progress + 0.05f);
        Draw.rect(plan.unit.fullIcon, sx, sy, rotdeg() - 90f);
        Draw.flush();
        Draw.color();
        Shaders.blockbuild.alpha = 1f;
      });

      Draw.reset();

      Draw.z(Layer.buildBeam);

      Draw.mixcol(Tmp.c1.set(Pal.accent).lerp(Pal.remove, invalidWarmup), 1f);
      Draw.alpha(Math.min(powerWarmup, sameTypeWarmup));
      Draw.rect(plan.unit.fullIcon, spawn.x, spawn.y, rotdeg() - 90f);

      Draw.alpha(Math.min(1f - invalidWarmup, warmup));

      for (var unit : units) {
        if (!((AssemblerAI) unit.controller()).inPosition())
          continue;
        float px = unit.x + Angles.trnsx(unit.rotation, unit.type.buildBeamOffset);
        float py = unit.y + Angles.trnsy(unit.rotation, unit.type.buildBeamOffset);
        Drawf.buildBeam(px, py, spawn.x, spawn.y, plan.unit.hitSize / 2f);
      }

      Fill.square(spawn.x, spawn.y, plan.unit.hitSize / 2f);

      Draw.reset();
      Draw.z(Layer.buildBeam);

      float fulls = areaSize * tilesize / 2f;
      Lines.stroke(2f, Pal.accent);
      Draw.alpha(powerWarmup);
      Drawf.dashRectBasic(spawn.x - fulls, spawn.y - fulls, fulls * 2f, fulls * 2f);

      Draw.reset();

      float outSize = plan.unit.hitSize + 9f;

      if (invalidWarmup > 0) {
        Lines.stroke(2f, Tmp.c3.set(Pal.accent).lerp(Pal.remove, invalidWarmup).a(invalidWarmup));
        Drawf.dashSquareBasic(spawn.x, spawn.y, outSize);
      }

      Draw.reset();
    }

    public void yeetPayload(Payload payload) {
      var spawn = getUnitSpawn();
      blocks.add(payload.content(), 1);
      float rot = payload.angleTo(spawn);
      Fx.shootPayloadDriver.at(payload.x(), payload.y(), rot);
      Fx.payloadDeposit.at(payload.x(), payload.y(), rot, new UnitAssembler.YeetData(spawn.cpy(), payload.content()));
      Sounds.shootPayload.at(x, y, 1f + Mathf.range(0.1f), 1f);
    }

    @Override
    public BlockStatus status() {
      if (!team.activateUnitFactories())
        return BlockStatus.inactiveUnitFactory;
      return super.status();
    }

    @Override
    public double sense(LAccess sensor) {
      if (sensor == LAccess.progress)
        return progress;
      return super.sense(sensor);
    }

    @Override
    public boolean acceptUnitPayload(Unit unit) {
      var plan = getPlan();
      if (plan == null || plan.requirements == null)
        return false;
      return plan.requirements.contains(b -> b.item == unit.type() &&
          blocks.get(unit.type()) < Mathf.round(b.amount * state.rules.unitCost(team)));
    }

    @Override
    public PayloadSeq getPayloads() {
      return blocks;
    }

    @Override
    public boolean acceptPayload(Building source, Payload payload) {
      var plan = getPlan();
      if (plan == null || plan.requirements == null)
        return false;
      return this.payload == null &&
          plan.requirements.contains(b -> b.item == payload.content() &&
              blocks.get(payload.content()) < Mathf.round(b.amount * state.rules.unitCost(team)));
    }

    @Override
    public int getMaximumAccepted(Item item) {
      return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
      var plan = getPlan();
      if (plan == null || plan.itemReq == null)
        return false;
      return items.get(item) < getMaximumAccepted(item) &&
          Structs.contains(plan.itemReq, stack -> stack.item == item);
    }

    @Override
    public Vec2 getCommandPosition() {
      return commandPos;
    }

    @Override
    public void onCommand(Vec2 target) {
      commandPos = target;
    }

    @Override
    public void buildConfiguration(Table table) {
      Seq<UnitType> units = Seq.with(plans)
          .map(p -> p.unit)
          .retainAll(u -> u != null && (!u.isBanned() || ignoreUnitBan));

      if (units.any()) {
        ItemSelection.buildTable(ModularUnitAssembler.this, table, units,
            () -> currentPlan == -1 || currentPlan >= plans.size || plans.get(currentPlan) == null
                ? null
                : plans.get(currentPlan).unit,
            unit -> {
              int next = plans.indexOf(p -> p != null && p.unit == unit);
              if (currentPlan != next) {
                currentPlan = next;
                progress = 0f;
              }
            },
            selectionRows, selectionColumns);

        table.row();

        Table commands = new Table();
        commands.top().left();

        Runnable rebuildCommands = () -> {
          commands.clear();
          commands.background(null);
          var unit = unit();
          if (unit != null && unit.commands.size > 1 && unit.allowChangeCommands
              && !(unit.commands.size == 2 && unit.commands.get(1) == UnitCommand.enterPayloadCommand)) {
            commands.background(Styles.black6);
            var group = new arc.scene.ui.ButtonGroup<ImageButton>();
            group.setMinCheckCount(0);
            int i = 0, columns = Mathf.clamp(units.size, 2, selectionColumns);
            var list = unit.commands;

            commands.image(Tex.whiteui, Pal.gray).height(4f).growX().colspan(columns).row();

            for (var item : list) {
              ImageButton button = commands.button(item.getIcon(), Styles.clearNoneTogglei, 40f, () -> {
                configure(item);
              }).tooltip(item.localized()).group(group).get();

              button
                  .update(() -> button.setChecked(command == item || (command == null && unit.defaultCommand == item)));

              if (++i % columns == 0) {
                commands.row();
              }
            }

            if (list.size < columns) {
              for (int j = 0; j < (columns - list.size); j++) {
                commands.add().size(40f);
              }
            }
          }
        };

        rebuildCommands.run();

        table.row();
        table.add(commands).fillX().left();

      } else {
        table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
      }
    }

    @Override
    public byte version() {
      return 2;
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.f(progress);
      write.b(units.size);
      for (var unit : units) {
        write.i(unit.id);
      }
      blocks.write(write);
      TypeIO.writeVecNullable(write, commandPos);
      write.s(currentPlan);
      TypeIO.writeCommand(write, command);
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      progress = read.f();
      int count = read.b();
      readUnits.clear();
      for (int i = 0; i < count; i++) {
        readUnits.add(read.i());
      }
      whenSyncedUnits.clear();
      blocks.read(read);
      if (revision >= 1) {
        commandPos = TypeIO.readVecNullable(read);
      }
      if (revision >= 2) {
        currentPlan = read.s();
        command = TypeIO.readCommand(read);
      }
    }
  }
}
