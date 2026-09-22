package mc.content;

import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.ctype.UnlockableContent;

public class Tech {
  private static TechNode context = null;

  public static void load() {
    addNode(Blocks.coreShard, MBlocks.mcf);
    addNode(MBlocks.mcf, MBlocks.lc);
    addNode(MBlocks.mcf, MBlocks.mmf);
    addNode(MBlocks.mcf, MUnits.mc3);
    addNode(MUnits.mc3, MUnits.mc2);
    addNode(MUnits.mc2, MUnits.moveCore1);
  }

  public static void addNode(UnlockableContent content, UnlockableContent child) {
    context = TechTree.all.find(t -> t.content == content);
    TechNode node = new TechNode(null, child, child.researchRequirements());
    if (!context.children.contains(node)) {
      context.children.add(node);
    }
    node.parent = context;
    node.planet = context.planet;
  }
}
