package mc.entities.mindustryX;

import arc.struct.Seq;
import mindustry.entities.units.StatusEntry;

public interface MindustryXUnitc {
    Seq<StatusEntry> statuses();

    float healthBalance();

    void healthChanged();
}
