package mc.content;

import mc.blocks.CoreConstructor;
import mc.blocks.ModularUnitAssembler;
import mc.blocks.MoveUpgradeFactory;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.type.PayloadStack;
import mindustry.world.Block;
import mindustry.world.blocks.payloads.Constructor;

import static mindustry.type.ItemStack.*;

public class MBlocks {
  public static Block mcf, lc, mmf;

  public static void load() {
    mcf = new ModularUnitAssembler("mcf") {
      {
        requirements(Category.units,
            with(Items.copper, 500, Items.thorium, 150, Items.plastanium, 280, Items.silicon, 650));
        size = 6;
        plans.add(
            new AssemblerUnitPlan(MUnits.mc3, 60f * 50f, 2,
                PayloadStack.list(UnitTypes.quasar, 4, Blocks.coreFoundation, 1)));
        areaSize = 13;
        researchCostMultiplier = 0.4f;

        consumePower(5f);
        consumeLiquid(Liquids.cryofluid, 9f / 60f);
      }
    };
    lc = new CoreConstructor("lc") {
      {
        requirements(Category.units,
            with(Items.silicon, 150, Items.titanium, 100, Items.lead, 200, Items.thorium, 80));
        hasPower = true;
        buildSpeed = 0.75f;
        maxBlockSize = 5;
        minBlockSize = 1;
        size = 5;
        regionSuffix = "-dark";
        consumePower(3f);
      }
    };
    mmf = new MoveUpgradeFactory("mmf") {
      {
        requirements(Category.units,
            with(Items.copper, 400, Items.lead, 350, Items.silicon, 320, Items.titanium, 280, Items.surgeAlloy, 120));
        size = 5;
        hasPower = true;
        hasItems = true;
        droneConstructTime = 60f * 2f;

        upgrades.add(new MoveUpgradeFactory.Upgrade(
            MUnits.mc3, MUnits.mc2,
            60f * 60f,
            ItemStack.with(Items.silicon, 500, Items.thorium, 400, Items.plastanium, 320, Items.titanium, 300),
            LiquidStack.with(Liquids.cryofluid, 0.5f)));

        upgrades.add(new MoveUpgradeFactory.Upgrade(
            MUnits.mc2, MUnits.moveCore1,
            60f * 80f,
            ItemStack.with(Items.silicon, 1000, Items.thorium, 600, Items.plastanium, 580, Items.titanium, 600,
                Items.surgeAlloy, 650, Items.phaseFabric, 500),
            LiquidStack.with(Liquids.cryofluid, 1f)));
      }
    };
  }
}
