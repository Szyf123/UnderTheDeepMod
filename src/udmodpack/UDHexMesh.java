package udmodpack;

import arc.math.geom.Vec3;
import mindustry.graphics.Shaders;
import mindustry.graphics.g3d.MeshBuilder;
import mindustry.graphics.g3d.PlanetMesh;
import mindustry.graphics.g3d.PlanetParams;
import mindustry.type.Planet;

/** 可自定义高度 intensity 的星球 mesh。
 *  继承 PlanetMesh（HexMesh 的父类），绕过 HexMesh 构造函数里 intensity=0.2f 的硬编码。
 *  手动补回 HexMesh.preRender() 逻辑 —— 把 planet 赋给 shader，否则 PlanetShader.apply() 会 NPE。 */
public class UDHexMesh extends PlanetMesh {
    /** @param divisions 球体细分次数（越高越精细）
     *  @param intensity  高度强度系数：顶点偏移量 = getHeight() 返回值 × intensity */
    public UDHexMesh(Planet planet, int divisions, float intensity) {
        super(planet,
            MeshBuilder.buildHex(planet.generator, divisions, planet.radius, intensity),
            Shaders.planet);
    }

    /** 抄自 HexMesh.preRender()。
     *  PlanetShader.apply() 依赖 Shaders.planet.planet / emissive / lightDir / ambientColor，
     *  这些必须在渲染前由 preRender 赋值。 */
    @Override
    public void preRender(PlanetParams params) {
        Shaders.planet.planet = planet;
        Shaders.planet.emissive = planet.generator != null && planet.generator.isEmissive();
        Shaders.planet.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
        Shaders.planet.ambientColor.set(planet.solarSystem.lightColor);
    }
}
