package mc.gen;

import arc.math.Mathf;
import arc.util.Tmp;
import arc.math.geom.Vec2;
import mindustry.type.UnitType;
import mindustry.gen.Legsc;
import arc.util.Time;
import mindustry.entities.Leg;

import mindustry.gen.Builderc;
import mindustry.gen.Drawc;
import mindustry.gen.Entityc;
import mindustry.gen.Healthc;
import mindustry.gen.Hitboxc;
import mindustry.gen.Itemsc;
import mindustry.gen.Legsc;
import mindustry.gen.Minerc;
import mindustry.gen.Physicsc;
import mindustry.gen.Posc;
import mindustry.gen.Rotc;
import mindustry.gen.Shieldc;
import mindustry.gen.Statusc;
import mindustry.gen.Syncc;
import mindustry.gen.Teamc;
import mindustry.gen.Unitc;
import mindustry.gen.Velc;
import mindustry.gen.Weaponsc;

@SuppressWarnings({ "all", "unchecked", "deprecation" })
public abstract interface RetractableLegsc extends Builderc, Drawc, Entityc, Healthc, Hitboxc, Itemsc, Legsc, Minerc,
        Physicsc, Posc, Rotc, Shieldc, Statusc, Syncc, Teamc, Unitc, Velc, Weaponsc {
    float defaultLegAngle(int index);

    float retractProgress();

    void updateRetractableLegs();
}
