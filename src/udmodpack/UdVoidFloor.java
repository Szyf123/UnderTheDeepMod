package udmodpack;

import arc.Core;
import arc.graphics.gl.Shader;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Time;
import mindustry.graphics.CacheLayer;
import arc.math.Mathf;
import mindustry.Vars;
import mindustry.world.blocks.environment.Floor;

/** Void floor: base tile + custom wave ShaderLayer (mimics vanilla water.frag). */
public class UdVoidFloor extends Floor {

    // Mindustry vanilla screenspace.vert
    private static final String VERT =
        "attribute vec4 a_position;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main(){\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position = a_position;\n" +
        "}\n";

    // Fragment shader: wave baseline + bezier dots
    // Scheduling is done in Java (apply()), shader only renders based on uniforms.
    private static final String FRAG =
        "#define HIGHP\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec2 u_campos;\n" +
        "uniform vec2 u_resolution;\n" +
        "uniform float u_time;\n" +
        "uniform vec2 u_mapsize;\n" +
        "uniform float u_curveStart;\n" +
        "uniform vec2 u_P0;\n" +
        "uniform vec2 u_P1;\n" +
        "uniform vec2 u_P2;\n" +
        "varying vec2 v_texCoords;\n" +
        "const float mscl = 40.0;\n" +
        "const float mth = 7.0;\n" +
        "vec2 bezier3(vec2 p0,vec2 p1,vec2 p2,float s){\n" +
        "    float om=1.0-s;return om*om*p0+2.0*om*s*p1+s*s*p2;\n" +
        "}\n" +
        "float gauss(float x,float c,float s){float d=x-c;return exp(-d*d/(2.0*s*s));}\n" +
        "void main(){\n" +
        "    vec2 c = v_texCoords;\n" +
        "    vec2 v = vec2(1.0/u_resolution.x, 1.0/u_resolution.y);\n" +
        "    vec2 coords = vec2(c.x / v.x + u_campos.x, c.y / v.y + u_campos.y);\n" +
        "    float stime = u_time / 5.0;\n" +
        "    vec4 sampled = texture2D(u_texture, c + vec2(sin(stime/3.0 + coords.y/0.75) * v.x, 0.0));\n" +
        "    vec3 color = sampled.rgb * vec3(0.9, 0.9, 1);\n" +
        "    float tester = mod((coords.x + coords.y*1.1 + sin(stime / 8.0 + coords.x/5.0 - coords.y/100.0)*2.0) +\n" +
        "                           sin(stime / 20.0 + coords.y/3.0) * 1.0 +\n" +
        "                           sin(stime / 10.0 - coords.y/2.0) * 2.0 +\n" +
        "                           sin(stime / 7.0 + coords.y/1.0) * 0.5 +\n" +
        "                           sin(coords.x / 3.0 + coords.y / 2.0) +\n" +
        "                           sin(stime / 20.0 + coords.x/4.0) * 1.0, mscl);\n" +
        "    if(tester < mth){\n" +
        "        color *= 1.5;\n" +
        "    }\n" +
        "    float localTime = u_time - u_curveStart;\n" +
        "    vec3 lightAcc=vec3(0.0);\n" +
        "    if(localTime>=0.0&&localTime<=360.0){\n" +
        "        float timeBright=gauss(localTime,180.0,60.0);\n" +
        "        for(int i=0;i<24;i++){\n" +
        "            float birth=float(i)*15.0;\n" +
        "            float s=(localTime-birth)/90.0;\n" +
        "            if(s<0.0||s>1.0)continue;\n" +
        "            vec2 cp=bezier3(u_P0,u_P1,u_P2,s);\n" +
        "            float d=distance(coords,cp);\n" +
        "            float br=gauss(s,0.5,0.165)*timeBright;\n" +
        "            float radius=60.0*(0.4+br*0.6);\n" +
        "            float gl=pow(max(0.0,1.0-d/radius),2.5);\n" +
        "            lightAcc+=vec3(0.0,0.2,0.1)*gl*3.0*br;\n" +
        "        }\n" +
        "    }\n" +
        "    color+=lightAcc;\n" +
        "    gl_FragColor = vec4(color.rgb, min(sampled.a * 100.0, 1.0));\n" +
        "}\n";

    // Java-side scheduling state: shader can't remember things between frames.
    private float curveStart = -9999f;
    private float nextTriggerTime = 0f;
    private final Vec2 P0 = new Vec2();
    private final Vec2 P1 = new Vec2();
    private final Vec2 P2 = new Vec2();

    public UdVoidFloor(String name) {
        super(name, 1);

        this.solid = true;
        this.drownTime = 30f;
        this.canShadow = false;
        this.placeableOn = false;
        this.drawEdgeIn = true;
        this.drawEdgeOut = false;
        this.edge = null;

        this.inEditor = true;
        this.alwaysUnlocked = true;
    }

    @Override
    public void load() {
        super.load();
        if (Vars.headless) return;

        try {
            Shader waveShader = new Shader(VERT, FRAG) {
                @Override
                public void apply() {
                    setUniformf("u_campos",
                        Core.camera.position.x - Core.camera.width / 2,
                        Core.camera.position.y - Core.camera.height / 2);
                    setUniformf("u_resolution",
                        Core.camera.width, Core.camera.height);
                    setUniformf("u_time", Time.time);

                    float mapW, mapH;
                    if (Vars.world != null && Vars.world.width() > 0) {
                        mapW = Vars.world.width() * Vars.tilesize;
                        mapH = Vars.world.height() * Vars.tilesize;
                    } else {
                        mapW = mapH = 4096f;
                    }
                    setUniformf("u_mapsize", mapW, mapH);

                    // Java-side cycle scheduling (stateless GLSL can't do this).
                    if (Time.time >= nextTriggerTime) {
                        curveStart = Time.time;
                        P0.set(Mathf.random() * mapW, Mathf.random() * mapH);
                        P1.set(Mathf.random() * mapW, Mathf.random() * mapH);
                        P2.set(Mathf.random() * mapW, Mathf.random() * mapH);
                        nextTriggerTime = curveStart + 900f + Mathf.random(0f, 900f);
                    }
                    setUniformf("u_curveStart", curveStart);
                    setUniformf("u_P0", P0.x, P0.y);
                    setUniformf("u_P1", P1.x, P1.y);
                    setUniformf("u_P2", P2.x, P2.y);
                }
            };

            this.cacheLayer = new CacheLayer.ShaderLayer(waveShader, false);
            CacheLayer.addLast(this.cacheLayer);
            Log.info("[UdVoidFloor] Shader compiled OK, layer id=" + this.cacheLayer.id);
        } catch (Exception e) {
            Log.err("[UdVoidFloor] Shader FAILED: " + e.getMessage());
        }
    }
}
