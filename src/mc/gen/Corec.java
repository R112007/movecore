package mc.gen;

import mindustry.game.Team;
import arc.graphics.Color;
import arc.Core;
import arc.Events;
import mindustry.Vars;
import arc.graphics.g2d.Draw;
import mindustry.entities.Effect;
import mindustry.type.ItemStack;
import mindustry.entities.units.WeaponMount;
import arc.util.Strings;
import mindustry.graphics.Pal;
import mindustry.content.Fx;
import arc.graphics.g2d.Fill;
import arc.util.Tmp;
import mindustry.entities.Units;
import mindustry.ai.UnitCommand;
import mindustry.ui.Styles;
import arc.struct.Seq;
import mindustry.ai.types.CommandAI;
import mindustry.entities.abilities.Ability;
import static mindustry.Vars.*;
import mindustry.game.EventType.*;
import mindustry.game.EventType.SaveWriteEvent;
import arc.math.Mathf;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.content.Items;
import mindustry.entities.Damage;
import arc.scene.ui.layout.Table;
import mindustry.type.Item;
import mindustry.gen.Unitc;
import mindustry.world.meta.BlockFlag;
import mindustry.gen.Minerc;
import arc.graphics.g2d.Lines;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.ui.Bar;
import arc.util.Nullable;
import mindustry.input.InputHandler;
import mindustry.type.UnitType;
import mindustry.gen.Building;
import arc.math.Angles;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.gen.Posc;
import mindustry.world.blocks.ExplosionShield;
import mindustry.world.Tile;
import mindustry.world.meta.StatUnit;
import mindustry.core.UI;
import arc.util.Time;
import arc.struct.ObjectSet;
import mindustry.world.modules.ItemModule;

import arc.struct.Seq;
import mindustry.gen.Builderc;
import mindustry.gen.Building;
import mindustry.gen.Drawc;
import mindustry.gen.Entityc;
import mindustry.gen.Healthc;
import mindustry.gen.Hitboxc;
import mindustry.gen.Itemsc;
import mindustry.gen.Minerc;
import mindustry.gen.Physicsc;
import mindustry.gen.Player;
import mindustry.gen.Posc;
import mindustry.gen.Rotc;
import mindustry.gen.Shieldc;
import mindustry.gen.Statusc;
import mindustry.gen.Syncc;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.gen.Unitc;
import mindustry.gen.Velc;
import mindustry.gen.Weaponsc;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.modules.ItemModule;

@SuppressWarnings({ "all", "unchecked", "deprecation" })
public abstract interface Corec extends Builderc, Drawc, Entityc, Healthc, Hitboxc, Itemsc, Minerc, Physicsc, Posc,
        Rotc, Shieldc, Statusc, Syncc, Teamc, Unitc, Velc, Weaponsc {
    Corec corec();

    Seq<Building> nearbyBuildCache();

    Seq<Building> nearbyBuilds();

    boolean accept();

    boolean acceptItem(Building source, Item item);

    boolean autoSwitched();

    boolean deployed();

    boolean interact(Player player);

    boolean mining();

    boolean playerUnitInRange();

    float auxiliaryRange();

    float cacheTime();

    float cacheX();

    float cacheY();

    float idleTimer();

    float realRad();

    float suckRange();

    int storageCapacity();

    int unitCapBonus();

    Unit currentInteractor();

    Item getOreItem(Tile tile);

    CoreBlock.CoreBuild proxy();

    ItemModule flowItems();

    ItemModule items();

    ItemModule savedItems();

    void accept(boolean accept);

    void autoSwitched(boolean autoSwitched);

    void auxiliaryRange(float auxiliaryRange);

    void cacheTime(float cacheTime);

    void cacheX(float cacheX);

    void cacheY(float cacheY);

    void corec(Corec corec);

    void currentInteractor(Unit currentInteractor);

    void deployed(boolean deployed);

    void destroy();

    void handleItem(Building source, Item item);

    void idleTimer(float idleTimer);

    void items(ItemModule items);

    void nearbyBuildCache(Seq<Building> nearbyBuildCache);

    void proxy(CoreBlock.CoreBuild proxy);

    void savedItems(ItemModule savedItems);

    void spawnPlayer(Player player);

    void storageCapacity(int storageCapacity);

    void suckRange(float suckRange);

    void takeItem(Unitc machine, Item item, int amount);

    void unitCapBonus(int unitCapBonus);

    void updateAutoCommand();

    void updateCatchItemFromPlayer();

    void updateClosestCore();
}
