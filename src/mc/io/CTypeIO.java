package mc.io;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.world.modules.ItemModule;

public class CTypeIO {
  public static void writeItemModule(Writes writes, ItemModule item) {
    item.write(writes);
  }

  public static ItemModule readItemModule(Reads reads) {
    ItemModule item = new ItemModule();
    item.read(reads);
    return item;
  }
}
