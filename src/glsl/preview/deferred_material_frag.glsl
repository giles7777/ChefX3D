/** Demo of using gbuffer rendering pass. Fragment shader */

#version 110

uniform sampler2D normalMap;
uniform sampler2D colourMap;
uniform float tileScale;

varying vec3 vpos;
varying vec3 vnormal;
varying vec3 vtangent;
varying vec3 vbinormal;
varying float transparency;

/*
 Used when no depth texture 

uniform vec2 planes;

vec3 float_to_color(float f)
{
    vec3 color;
    f *= 256.0;
    color.r = floor(f);
    f = (f - color.r) * 256.0;
    color.g = floor(f);
    color.b = f - color.y;
    color.rg *= 0.00390625; // *= 1.0/256

    return color;
}

*/

void main(){
    if(transparency < 0.95)
        discard;

    vec2 tx_coord = gl_TexCoord[0].st * tileScale;

    // look up the normal map
    vec3 normal = texture2D(normalMap, tx_coord).xyz;

    // transform the normal to view space
    normal -= 0.5;
    normal = normalize(normal.x * vtangent + 
                       normal.y * vbinormal +
                       normal.z * vnormal);

    // Now back to colour space of [0, 1]
    normal = normal * 0.5 + 0.5;

    // look up the colour map
    vec4 colour = texture2D(colourMap, tx_coord);

    // Finally fill out the buffer passes
    gl_FragData[0] = vec4((colour * gl_FrontMaterial.diffuse).rgb, 1.0);
    gl_FragData[1] = vec4(normal, 1);
    gl_FragData[2] = vec4(gl_FrontMaterial.specular.rgb,
                          gl_FrontMaterial.shininess);

/*
 Use when no depth texture support 
    //float d = (planes.x * vpos.z + planes.y) / -viewSpacePosition.z;
    //gl_FragData[3] = vec4(float_to_color(d), 1.0);
*/
}
