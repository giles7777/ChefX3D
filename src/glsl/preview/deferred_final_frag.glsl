/** Demo of using an Anti-aliasing blur pass based on edge detection
in a deferred shading system. Fragment shader */

#version 110

uniform sampler2D baseMap;
//uniform sampler2D effectMap;
uniform sampler2D ssoaMap;

void main()
{
    vec3 base = texture2D(baseMap, gl_TexCoord[0].st).rgb;
//    vec3 effect = texture2D(effectMap, gl_TexCoord[0].st).rgb;
    vec3 ssoa = texture2D(ssoaMap, gl_TexCoord[0].st).rgb;

    // Basic additive blending for now
//    gl_FragColor = vec4(base + effect, 1.0);
//    gl_FragColor = vec4((base + effect) * ssoa, 1.0);
//    gl_FragColor = vec4(base, 1.0);
    gl_FragColor = vec4(base * ssoa, 1.0);
}
