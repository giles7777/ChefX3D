/** Demo of using the light pass in a deferred shading system. Fragment shader */

#version 110

uniform sampler2D normalMap;
uniform float weight;
uniform vec2 texSize;

void main()
{
    const int NUM_SAMPLES = 8;
    const float texel = 1.0;

    vec2 filter[NUM_SAMPLES];
    filter[0] = vec2(-texel,  texel);
    filter[1] = vec2( texel, -texel);
    filter[2] = vec2(-texel,  texel);
    filter[3] = vec2(-texel, -texel);

    filter[4] = vec2(-texel, 0.0);
    filter[5] = vec2( texel, 0.0);
    filter[6] = vec2(0.0,   -texel);
    filter[7] = vec2(0.0,   -texel);

    vec4 tex = texture2D(normalMap, gl_TexCoord[0].xy);
    float factor = 0.0;

    for(int i = 0; i < NUM_SAMPLES; i++)
    {
        vec4 t = texture2D(normalMap, gl_TexCoord[0].xy + filter[i] / texSize);
        t -= tex;

        factor += dot(t, t);
    }

    factor = min(1.0, factor) * weight;

    vec4 colour = vec4(1.0, 1.0, 1.0, 1.0);

    if (factor > 0.1) {
      colour.r = 0.0;
      colour.g = 0.0;
      colour.b = 0.0;
	}

    gl_FragColor = colour;
}
