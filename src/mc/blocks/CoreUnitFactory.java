package mc.blocks;

import arc.*;
import arc.audio.Sound;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import static mindustry.Vars.*;

public class CoreUnitFactory extends PayloadBlock {
  /** 单计划模式：直接指定单位类型 */
  @Nullable
  public UnitType unitType;
  /** 单计划模式：物品需求 */
  @Nullable
  public ItemStack[] unitRequirements;
  /** 单计划模式：最大单位数 */
  public int unitMax = 1;

  /** 多计划模式 */
  public Seq<CoreUnitPlan> plans = new Seq<>();

  /** 是否忽略单位被ban的限制 */
  public boolean ignoreUnitBan = true;

  public Effect spawnEffect = Fx.spawn;
  public Sound spawnSound = Sounds.unitCreate;
  public float buildTime = 60f;

  protected int[][] capacities = {};

  public CoreUnitFactory(String name) {
    super(name);
    update = true;
    solid = true;
    hasItems = true;
    hasPower = true;
    configurable = true;
    clearOnDoubleTap = true;
    outputsPayload = true;
    rotate = true;
    ambientSound = Sounds.loopUnitBuilding;
    ambientSoundVolume = 0.09f;

    // 配置切换计划
    config(Integer.class, (CoreUnitFactoryBuild build, Integer i) -> {
      if (!configurable)
        return;
      if (build.currentPlan == i)
        return;
      build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
      build.progress = 0f;
    });

    config(UnitType.class, (CoreUnitFactoryBuild build, UnitType val) -> {
      if (!configurable)
        return;
      int next = plans.indexOf(p -> p.unit == val);
      if (build.currentPlan == next)
        return;
      build.currentPlan = next;
      build.progress = 0f;
    });

    configClear((CoreUnitFactoryBuild build) -> {
      build.currentPlan = -1;
      build.progress = 0f;
    });
  }

  @Override
  public void init() {
    // 单计划转多计划
    if (plans.isEmpty() && unitType != null) {
      plans.add(new CoreUnitPlan(unitType, unitMax,
          unitRequirements != null ? unitRequirements : ItemStack.empty));
    }

    for (var plan : plans) {
      if (plan.requirements == null) {
        plan.requirements = ItemStack.empty;
      }
    }

    initCapacities();

    // 设置动态物品消耗 - 根据当前选择的计划消耗对应物品
    consume(new ConsumeItemDynamic((CoreUnitFactoryBuild e) -> {
      if (e.currentPlan < 0 || e.currentPlan >= plans.size)
        return ItemStack.empty;
      CoreUnitPlan plan = plans.get(e.currentPlan);
      return plan.requirements == null ? ItemStack.empty : plan.requirements;
    }));

    super.init();
  }

  @Override
  public void afterPatch() {
    initCapacities();
    super.afterPatch();
  }

  public void initCapacities() {
    capacities = new int[plans.size][Vars.content.items().size];
    itemCapacity = 10; // 默认容量，会被下面的计算覆盖

    for (int i = 0; i < plans.size; i++) {
      CoreUnitPlan plan = plans.get(i);
      if (plan.requirements == null || plan.requirements.length == 0)
        continue;

      for (ItemStack stack : plan.requirements) {
        if (stack == null || stack.item == null)
          continue;
        capacities[i][stack.item.id] = Math.max(capacities[i][stack.item.id], stack.amount * 2);
        itemCapacity = Math.max(itemCapacity, stack.amount * 2);
      }
    }
  }

  @Override
  public void setBars() {
    super.setBars();
    addBar("progress", (CoreUnitFactoryBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));

    addBar("units", (CoreUnitFactoryBuild e) -> {
      CoreUnitPlan plan = e.getPlan();
      return new Bar(
          () -> plan == null ? "[lightgray]" + Iconc.cancel
              : Core.bundle.format("bar.unitcap",
                  Fonts.getUnicodeStr(plan.unit.name),
                  e.team.data().countType(plan.unit),
                  plan.maxUnits),
          () -> Pal.power,
          () -> plan == null ? 0f : (float) e.team.data().countType(plan.unit) / plan.maxUnits);
    });
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
          if (plan.unit.isBanned() && !ignoreUnitBan) {
            t.image(Icon.cancel).color(Pal.remove).size(40);
            return;
          }

          t.image(plan.unit.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit);
          t.table(info -> {
            info.add(plan.unit.localizedName).left();
            info.row();
            info.add("Max: " + plan.maxUnits).color(Color.lightGray);
            if (ignoreUnitBan) {
              info.row();
              info.add("[accent]Ignore Ban").color(Pal.accent);
            }
          }).left();

          if (plan.requirements != null && plan.requirements.length > 0) {
            t.table(req -> {
              req.right();
              for (int i = 0; i < plan.requirements.length; i++) {
                if (i % 6 == 0)
                  req.row();
                ItemStack stack = plan.requirements[i];
                if (stack == null || stack.item == null)
                  continue;
                req.add(StatValues.displayItem(stack.item, stack.amount, buildTime, true)).pad(5);
              }
            }).right().grow().pad(10f);
          }
        }).growX().pad(5);
        table.row();
      }
    });
  }

  @Override
  public TextureRegion[] icons() {
    return new TextureRegion[] { region, outRegion, topRegion };
  }

  @Override
  public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
    Draw.rect(region, plan.drawx(), plan.drawy());
    Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
    Draw.rect(topRegion, plan.drawx(), plan.drawy());
  }

  @Override
  public void getPlanConfigs(Seq<UnlockableContent> options) {
    for (var plan : plans) {
      if (plan != null && plan.unit != null && (!plan.unit.isBanned() || ignoreUnitBan)) {
        options.add(plan.unit);
      }
    }
  }

  public static class CoreUnitPlan {
    public UnitType unit;
    public ItemStack[] requirements;
    public int maxUnits;

    public CoreUnitPlan(UnitType unit, int maxUnits, ItemStack... requirements) {
      this.unit = unit;
      this.maxUnits = maxUnits;
      this.requirements = requirements;
    }

    CoreUnitPlan() {
    }
  }

  public class CoreUnitFactoryBuild extends PayloadBlockBuild<UnitPayload> {
    public float progress;
    public float time;
    public float speedScl;
    public int currentPlan = -1;

    public CoreUnitPlan getPlan() {
      return currentPlan < 0 || currentPlan >= plans.size ? null : plans.get(currentPlan);
    }

    public float fraction() {
      return currentPlan == -1 ? 0f : progress / buildTime;
    }

    /** 检查是否达到单位上限 */
    public boolean isAtUnitCap() {
      CoreUnitPlan plan = getPlan();
      if (plan == null)
        return true;
      return team.data().countType(plan.unit) >= plan.maxUnits;
    }

    /** 检查单位是否被ban */
    public boolean isUnitBanned() {
      CoreUnitPlan plan = getPlan();
      if (plan == null)
        return true;
      if (ignoreUnitBan)
        return false;
      return plan.unit.isBanned();
    }

    @Override
    public void created() {
      // 自动选择第一个可用计划
      if (currentPlan == -1) {
        currentPlan = plans.indexOf(p -> p != null && p.unit != null && (!p.unit.isBanned() || ignoreUnitBan));
      }
    }

    @Override
    public void updateTile() {
      // 不可配置时锁定第一个计划
      if (!configurable) {
        currentPlan = 0;
      }

      // 验证计划索引
      if (currentPlan < 0 || currentPlan >= plans.size) {
        currentPlan = -1;
      }

      // 有电力且效率>0且计划有效时，积累进度
      if (efficiency > 0 && currentPlan != -1 && !isAtUnitCap() && !isUnitBanned()) {
        time += edelta() * speedScl * Vars.state.rules.unitBuildSpeed(team);
        progress += edelta() * Vars.state.rules.unitBuildSpeed(team);
        speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f);
      } else {
        speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
      }

      // 尝试输出已完成的单位payload
      moveOutPayload();

      // 进度达到建造时间且没有待输出的payload时，生产新单位
      if (currentPlan != -1 && payload == null) {
        CoreUnitPlan plan = getPlan();

        // 检查上限和ban状态
        if (plan == null || isAtUnitCap() || isUnitBanned()) {
          progress = 0f;
          return;
        }

        // 确保单位类型有效
        if (plan.unit == null || plan.unit.constructor == null) {
          progress = 0f;
          return;
        }

        if (progress >= buildTime) {
          progress %= 1f;

          // 创建单位并包装成Payload
          Unit unit = plan.unit.create(team);
          unit.set(x, y);
          unit.rotation(rotation * 90f);

          payload = new UnitPayload(unit);
          payVector.setZero();

          // 触发消耗（物品在consume()中自动消耗）
          consume();

          spawnSound.at(this, 1f + Mathf.range(0.06f), 1f);

          Events.fire(new UnitCreateEvent(payload.unit, this));
        }
      }

      // 限制进度不超过buildTime
      progress = Mathf.clamp(progress, 0f, buildTime);
    }

    @Override
    public boolean shouldConsume() {
      if (currentPlan == -1)
        return false;
      if (isAtUnitCap())
        return false;
      if (isUnitBanned())
        return false;
      return enabled && payload == null;
    }

    @Override
    public int getMaximumAccepted(Item item) {
      if (isAtUnitCap())
        return 0;
      if (isUnitBanned())
        return 0;
      if (currentPlan < 0 || currentPlan >= plans.size)
        return 0;
      return Mathf.round(capacities[currentPlan][item.id] * state.rules.unitCost(team));
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
      // 达到上限或被ban时不接受物品
      if (isAtUnitCap())
        return false;
      if (isUnitBanned())
        return false;

      if (currentPlan < 0 || currentPlan >= plans.size)
        return false;

      CoreUnitPlan plan = plans.get(currentPlan);
      if (plan == null || plan.requirements == null || plan.requirements.length == 0)
        return false;

      // 检查该物品是否是当前计划所需的
      boolean isRequired = false;
      for (ItemStack stack : plan.requirements) {
        if (stack != null && stack.item == item) {
          isRequired = true;
          break;
        }
      }

      return isRequired && items.get(item) < getMaximumAccepted(item);
    }

    @Override
    public void buildConfiguration(Table table) {
      Seq<UnitType> units = Seq.with(plans)
          .map(p -> p.unit)
          .retainAll(u -> u != null && (!u.isBanned() || ignoreUnitBan));

      if (units.any()) {
        ItemSelection.buildTable(CoreUnitFactory.this, table, units,
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
      } else {
        table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
      }
    }

    @Override
    public void draw() {
      Draw.rect(region, x, y);
      Draw.rect(outRegion, x, y, rotdeg());

      CoreUnitPlan plan = getPlan();
      if (plan != null && !isAtUnitCap() && !isUnitBanned() && progress > 0) {
        Draw.draw(Layer.blockOver, () -> {
          Drawf.construct(this, plan.unit, rotdeg() - 90f, fraction(), speedScl, time);
        });
      }

      Draw.z(Layer.blockOver);
      payRotation = rotdeg();
      drawPayload();

      Draw.z(Layer.blockOver + 0.1f);
      Draw.rect(topRegion, x, y);
    }

    @Override
    public void display(Table table) {
      super.display(table);

      CoreUnitPlan plan = getPlan();
      if (plan != null) {
        table.row();
        table.table(t -> {
          t.left();
          t.image(plan.unit.uiIcon).size(32).padRight(4);
          t.label(() -> {
            String text = plan.unit.localizedName + " (" + team.data().countType(plan.unit) + "/" + plan.maxUnits + ")";
            if (isUnitBanned() && !ignoreUnitBan) {
              text += " [red][Banned]";
            } else if (ignoreUnitBan) {
              text += " [accent][Ignore Ban]";
            }
            return text;
          }).wrap().width(230f).color(Color.lightGray);
        }).left();
      }
    }

    @Override
    public Object config() {
      return currentPlan;
    }

    @Override
    public byte version() {
      return 1;
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.f(progress);
      write.s(currentPlan);
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      if (revision >= 1) {
        progress = read.f();
        currentPlan = read.s();
      }
    }
  }
}
