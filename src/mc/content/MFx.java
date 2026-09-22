package mc.content;

import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Angles;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.util.Tmp;
import arc.util.noise.Simplex;
import mindustry.entities.Effect;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.ParticleEffect;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;

public class MFx {
  public static Effect energyField = new Effect(140f, 360f, e -> {
    e.color = Pal.heal;
    float radius = 64f;
    float fin = e.fin();
    float fout = e.fout();
    float alpha = fout * e.color.a;
    Color c = e.color;

    // ===== 0. 加法混合：所有发光层叠加 =====
    Draw.blend(Blending.additive);

    // ===== 1. 外层大气辉光（Fill.light 径向渐变） =====
    Draw.color(c, alpha * 0.35f);
    Fill.light(e.x, e.y, 28, radius * 3.0f * fout, c, Color.clear);

    // ===== 2. 中心径向光晕 + 极亮核 =====
    Draw.color(c, alpha);
    Fill.light(e.x, e.y, 32, radius * 0.65f * fout, Color.white, c);

    float corePulse = 1f + Mathf.absin(fin * 10f, 10f, 0.3f);
    Draw.color(Color.white, alpha);
    Fill.circle(e.x, e.y, radius * 0.18f * corePulse * (1f + fin * 0.25f));
    Draw.color(Color.white, alpha * 0.95f);
    Fill.circle(e.x, e.y, radius * 0.09f * corePulse);

    // ===== 3. 多层旋转光环 =====
    // 内环：白色，较粗。半径改为随 fout 收缩，alpha 用二次衰减，后期消失更快更柔
    float ringFade = fout * fout;
    Lines.stroke(3.0f * ringFade);
    Draw.color(Color.white, alpha * 0.95f * ringFade);
    Lines.circle(e.x, e.y, radius * (0.52f - 0.22f * fout));

    // 中环：主题色
    Lines.stroke(2.0f * fout);
    Draw.color(c, alpha * 0.85f);
    Lines.circle(e.x, e.y, radius * (0.48f + 0.30f * fin));

    // 外环：淡蓝，较细
    Lines.stroke(1.4f * fout);
    Draw.color(Color.sky, alpha * 0.65f);
    Lines.circle(e.x, e.y, radius * (0.68f + 0.24f * fin));

    // ===== 4. 环绕能量带（厚发光环）======
    // 关键修复：Fill.lightInner 直接读取传入的颜色，不受 Draw.color 影响。
    // 原来内圈是 Color.white（alpha 始终为 1），所以白环不会随整体 alpha 淡出。
    // 现在把内圈 alpha 绑到 bandAlpha 上，外圈保持 Color.clear，就能真正渐变消失。
    float bandAlpha = alpha * 0.45f * ringFade;
    Fill.lightInner(e.x, e.y, 40,
        radius * (0.55f - 0.15f * fout),
        radius * (0.75f - 0.20f * fout),
        fin * 90f + e.rotation,
        Tmp.c1.set(Color.white).a(bandAlpha),
        Color.clear);

    // ===== 5. 3D 球面粒子层 =====
    int sphereParticles = 90;
    for (int i = 0; i < sphereParticles; i++) {
      long s = e.id * 0x9E3779B97F4A7C15L + i * 2654435761L;

      float theta = Mathf.randomSeed(s, 0f, Mathf.PI2);
      float z = Mathf.randomSeed(s + 1, -1f, 1f);
      float rXY = Mathf.sqrt(1f - z * z);

      float px = rXY * Mathf.cos(theta);
      float py = rXY * Mathf.sin(theta);
      float pz = z;

      float rot = fin * 45f + e.rotation;
      float rx = px * Mathf.cosDeg(rot) - pz * Mathf.sinDeg(rot);
      float rz = px * Mathf.sinDeg(rot) + pz * Mathf.cosDeg(rot);

      float size = Mathf.lerp(0.5f, 2.0f, (rz + 1f) / 2f) * fout;
      float pAlpha = alpha * Mathf.lerp(0.3f, 1f, (rz + 1f) / 2f);

      Draw.color(Tmp.c1.set(Color.white).lerp(c, 1f - (rz + 1f) / 2f).a(pAlpha));
      Fill.circle(
          e.x + rx * radius * 0.75f * fout,
          e.y + py * radius * 0.75f * fout,
          size);
    }

    // ===== 6. 圆盘随机粒子（补充密度） =====
    Draw.color(Tmp.c1.set(Color.white).lerp(c, 0.4f).a(alpha));
    Angles.randLenVectors(e.id, 45, radius * 0.95f * fout, (x, y) -> {
      float dist = Mathf.dst(x, y);
      float depth = 1f - Mathf.clamp(dist / (radius * 0.95f));
      float pSize = Mathf.lerp(0.5f, 1.8f, depth) * fout;
      float pAlpha = alpha * Mathf.lerp(0.35f, 0.9f, depth);

      Draw.color(Tmp.c1.set(Color.white).lerp(c, 1f - depth).a(pAlpha));
      Fill.circle(e.x + x, e.y + y, pSize);
    });

    // ===== 7. 放射状能量触须（带 Simplex 噪声抖动） =====
    int tendrils = 14;
    for (int i = 0; i < tendrils; i++) {
      long seed = e.id * 41L + i * 17L;
      float baseAngle = i * 360f / tendrils + e.rotation + fin * 75f;
      float len = radius * (1.0f + 1.1f * fin);

      Draw.color(Tmp.c1.set(Color.white).lerp(c, fin).a(alpha * (1f - fin * 0.25f)));
      Lines.stroke(Mathf.lerp(1.4f, 0.5f, fin) * fout);

      Lines.beginLine();
      Lines.linePoint(e.x, e.y);

      int segs = 18;
      for (int j = 1; j <= segs; j++) {
        float t = j / (float) segs;
        float nx = seed * 0.017f + j * 0.31f;
        float ny = fin * 2.8f + j * 0.19f;
        float jitter = Simplex.noise2d((int) seed, 2, 0.5f, 1.5f, nx, ny) * 32f * t * t;
        float angle = baseAngle + jitter;
        float r = len * t;

        Lines.linePoint(
            e.x + Angles.trnsx(angle, r),
            e.y + Angles.trnsy(angle, r));
      }
      Lines.endLine(false);
    }

    // ===== 8. 环绕螺旋电弧（贴合球面） =====
    int streams = 6;
    for (int i = 0; i < streams; i++) {
      long seed = e.id * 73L + i * 29L;
      float baseAngle = i * 360f / streams + e.rotation * 0.5f - fin * 120f;
      float orbitRadius = radius * (0.55f + 0.25f * fin);

      Draw.color(c, alpha * 0.7f);
      Lines.stroke(1.2f * fout);
      Lines.beginLine();

      int segs = 24;
      for (int j = 0; j <= segs; j++) {
        float t = j / (float) segs;
        float angle = baseAngle + t * 180f;
        float nx = seed * 0.013f + j * 0.21f;
        float ny = fin * 3f + t * 1.5f;
        float wobble = Simplex.noise2d((int) seed, 2, 0.5f, 1.2f, nx, ny) * 18f;
        float r = orbitRadius + wobble * (1f - Math.abs(t - 0.5f) * 2f);

        Lines.linePoint(
            e.x + Angles.trnsx(angle + wobble, r),
            e.y + Angles.trnsy(angle + wobble, r));
      }
      Lines.endLine(false);
    }

    // ===== 9. 环境点光源 =====
    Drawf.light(e.x, e.y, radius * 3.6f, c, alpha * 0.8f);

    // ===== 10. 恢复 =====
    Draw.blend();
    Draw.reset();
  }).layer(Layer.effect + 0.1f);
  public static Effect energyParticleField = new MultiEffect(energyField, new ParticleEffect() {
    {
      lifetime = 140f;
      layer = Layer.effect;
      clip = 60f;
      particles = 22;
      cone = 360f;
      length = 120f;
      baseLength = 5f;
      randLength = true;
      colorFrom = Pal.heal;
      colorTo = Color.white;
      interp = Interp.pow2Out;
      sizeInterp = Interp.pow2Out;
      sizeFrom = 4f;
      sizeTo = 2f;
      sizeChangeStart = 0f;
    }
  });
}
