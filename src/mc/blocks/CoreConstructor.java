package mc.blocks;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.Styles;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 可以生产核心（CoreBlock）的建筑工厂。
 * 核心只能输出到 ModularUnitAssembler，其他建筑/空地不能接收核心。
 * 单位无法直接从里面搬运建筑。
 */
public class CoreConstructor extends BlockProducer {
  public Seq<Block> filter = new Seq<>();
  public int minBlockSize = 1, maxBlockSize = 2;

  public CoreConstructor(String name) {
    super(name);

    size = 3;
    configurable = true;
    clearOnDoubleTap = true;
    acceptsUnitPayloads = false; // 单位无法直接搬运

    configClear((CoreConstructorBuild tile) -> tile.recipe = null);
    config(Block.class, (CoreConstructorBuild tile, Block block) -> {
      if (tile.recipe != block)
        tile.progress = 0f;
      if (canProduce(block)) {
        tile.recipe = block;
      }
    });
  }

  @Override
  public void setStats() {
    super.setStats();
    stats.add(Stat.output, "@x@ ~ @x@", minBlockSize, minBlockSize, maxBlockSize, maxBlockSize);
    stats.addPercent(Stat.buildSpeed, buildSpeed);
  }

  @Override
  public void getPlanConfigs(Seq<UnlockableContent> options) {
    // 普通建筑走 canProduce
    content.blocks().each(this::canProduce, options::addUnique);
    // 核心强制加入（去重）
    content.blocks().each(b -> b instanceof CoreBlock, options::addUnique);
  }

  public boolean canProduce(Block b) {
    return b.isVisible()
        && b.size >= minBlockSize
        && b.size <= maxBlockSize
        && !state.rules.isBanned(b)
        && b.environmentBuildable()
        && (filter.isEmpty() || filter.contains(b));
  }

  public class CoreConstructorBuild extends BlockProducerBuild {
    public @Nullable Block recipe;

    public boolean canProduce(Block b) {
      return b.isVisible()
          && b.size >= minBlockSize
          && b.size <= maxBlockSize
          && !state.rules.isBanned(b)
          && b.environmentBuildable()
          && (filter.isEmpty() || filter.contains(b));
    }

    @Override
    public void buildConfiguration(Table table) {
      Seq<Block> blocks = new Seq<>();
      // 普通建筑走 canProduce
      content.blocks().each(this::canProduce, blocks::addUnique);
      // 核心强制加入
      // content.blocks().each(b -> b == Blocks.coreShard, blocks::addUnique);

      ButtonGroup<ImageButton> group = new ButtonGroup<>();
      group.setMinCheckCount(0);
      Table cont = new Table().top();
      cont.defaults().size(40);

      TextField search = new TextField("");
      search.setMessageText("@players.search");

      int cols = selectionColumns;
      int rows = selectionRows;

      Runnable rebuild = () -> {
        group.clear();
        cont.clearChildren();

        String text = search.getText().toLowerCase();
        int i = 0, rowCount = 0;

        for (Block b : blocks) {
          if (!b.unlockedNow())
            continue;
          // 不检查 isHidden() 和 isOnPlanet()，否则核心被过滤
          if (!text.isEmpty() && !b.localizedName.toLowerCase().contains(text))
            continue;

          ImageButton button = cont.button(Tex.whiteui, Styles.clearNoneTogglei,
              Mathf.clamp(b.selectionSize, 0f, 40f), () -> {
                control.input.config.hideConfig();
              }).tooltip(b.localizedName).group(group).get();

          button.changed(() -> {
            if (button.isChecked()) {
              if (b != recipe)
                progress = 0f;
              configure(b);
            } else {
              configure(null);
            }
          });
          button.getStyle().imageUp = new TextureRegionDrawable(b.uiIcon);
          button.update(() -> button.setChecked(recipe == b));

          if (++i % cols == 0) {
            cont.row();
            rowCount++;
          }
        }
      };

      rebuild.run();

      Table main = new Table().background(Styles.black6);
      if (blocks.size > cols * rows * 1.5f) {
        main.table(s -> {
          s.image(Icon.zoom).padLeft(4f);
          s.add(search).padBottom(4).left().growX();
          search.changed(rebuild);
        }).fillX().row();
      }

      ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
      pane.setScrollingDisabled(true, false);
      pane.setOverscroll(false, false);
      pane.exited(() -> {
        if (pane.hasScroll())
          Core.scene.setScrollFocus(null);
      });

      pane.setScrollYForce(selectScroll);
      pane.update(() -> selectScroll = pane.getScrollY());

      main.add(pane).maxHeight(40 * rows);
      table.top().add(main);
    }

    @Override
    public @Nullable Block recipe() {
      return recipe;
    }

    @Override
    public Object config() {
      return recipe;
    }

    @Override
    public void drawSelect() {
      if (recipe != null) {
        float dx = x - size * tilesize / 2f, dy = y + size * tilesize / 2f;
        TextureRegion icon = recipe.uiIcon;
        Draw.mixcol(Color.darkGray, 1f);
        Draw.rect(icon, dx - 0.7f, dy - 1f, Draw.scl * Draw.xscl * 24f, Draw.scl * Draw.yscl * 24f);
        Draw.reset();
        Draw.rect(icon, dx, dy, Draw.scl * Draw.xscl * 24f, Draw.scl * Draw.yscl * 24f);
      }
    }

    /**
     * 重写输出逻辑：
     * - 如果 payload 是核心（CoreBlock），只能输出到前方是 ModularUnitAssembler 的情况
     * - 其他方块正常输出
     */
    @Override
    public void moveOutPayload() {
      if (payload == null)
        return;

      updatePayload();

      Vec2 dest = Tmp.v1.trns(rotdeg(), size * tilesize / 2f);
      payRotation = Angles.moveToward(payRotation, rotdeg(), payloadRotateSpeed * delta());
      payVector.approach(dest, payloadSpeed * delta());

      Building front = front();
      boolean canDump = front == null || !front.tile.solid();
      boolean canMove = front != null && (front.block.outputsPayload || front.block.acceptsPayload);

      // 核心限制：CoreBlock 只能输出到 ModularUnitAssembler
      if (payload instanceof BuildPayload bp && bp.build.block instanceof CoreBlock) {
        canMove = front != null && front.block instanceof ModularUnitAssembler;
        canDump = false; // 核心不能随意丢弃到空地
      }

      if (canDump && !canMove) {
        pushOutput(payload, 1f - (payVector.dst(dest) / (size * tilesize / 2f)));
      }

      if (payVector.within(dest, 0.001f)) {
        payVector.clamp(-size * tilesize / 2f, -size * tilesize / 2f, size * tilesize / 2f, size * tilesize / 2f);

        if (canMove) {
          if (movePayload(payload)) {
            payload = null;
          }
        } else if (canDump) {
          dumpPayload();
        }
      }
    }

    @Override
    public void write(Writes write) {
      super.write(write);
      write.s(recipe == null ? -1 : recipe.id);
    }

    @Override
    public void read(Reads read, byte revision) {
      super.read(read, revision);
      recipe = Vars.content.block(read.s());
    }
  }
}
