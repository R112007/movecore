package mc.meta;

import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;

public class CStat {
  public static final Stat storageCapacity;
  public static final Stat suckRange;

  static {
    storageCapacity = new Stat("storageCapacity", StatCat.items);
    suckRange = new Stat("suckRange");
  }

}
