package mc.game;

import mindustry.maps.Map;

public class MEventTypes {
  public static class MapChangeEvent {
    public Map map;

    public MapChangeEvent(Map map) {
      this.map = map;
    }
  }
}
