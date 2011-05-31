/** Demo of using the light pass in a deferred shading system. Fragment shader */

#version 110

uniform sampler2D diffuseMap;
uniform sampler2D specularMap;
uniform sampler2D depthMap;
uniform sampler2D normalMap;

uniform vec3 lightPos;
uniform float lightRadius;
uniform vec3 lightColor;
uniform vec2 planes;
uniform vec3 attenuation;

varying vec3 viewCoordsPosition;
varying vec3 viewSpacePosition;

float saturate(float x)
{
    return max(0.0, min(1.0, x));
}

vec4 lighting(vec3 pos, vec3 normal, vec3 diffuse, vec3 specular, float shininess)
{
    vec3 l = lightPos - pos;
    vec3 v = normalize(pos);
    vec3 h = normalize(v + l);

    // attenuation calc
//    float att = saturate(1.0 - length(l) / lightRadius);
    float d = 1.0 - length(l);

    float att = saturate(attenuation.x + 
                         attenuation.y * d +
                         attenuation.z * d * d);

//    float att = saturate(1.0 / (attenuation.x + 
//                                attenuation.y * d +
//                                attenuation.z * d * d));
    l = normalize(l);

    vec3 diffuse_coeff = saturate(dot(l, normal)) * diffuse * lightColor;
    vec3 spec_coeff = pow(saturate(dot(h, normal)), shininess) * specular * lightColor;

    return vec4(att * (diffuse_coeff + spec_coeff), 1.0);
}

/*
  Use when no depth texture support 
float color_to_float(vec3 color) 
{
    const vec3 byte_to_float = vec3(1.0, 1.0 / 256.0, 1.0 / (256.0 * 256.0));
    return dot(color, byte_to_float);
}

void main()
{
    vec3 depth_color = texture2D(depthMap, gl_TexCoord[0].st).rgb;
    float depth = color_to_float(depth_color);
*/

void main()
{
    float depth = texture2D(depthMap, gl_TexCoord[0].st).r;

    vec3 view = normalize(viewCoordsPosition);
    vec3 pos = vec3(0.0);

    pos.z = -planes.y / (planes.x + depth);
    pos.xy = view.xy / view.z * pos.z;

    // normal was in [0,1] colour space, so convert it back to [-1. 1]
    vec3 normal = (texture2D(normalMap, gl_TexCoord[0].st).rgb - 0.5) * 2.0;
    float len = length(normal);

    if(len > 0.01)
       normal /= len;
    else
       normal = vec3(0.0);

    vec3 diffuse = texture2D(diffuseMap, gl_TexCoord[0].st).rgb;
    vec4 specular = texture2D(specularMap, gl_TexCoord[0].st);

    if(depth > 0.9999)
        gl_FragColor = vec4(diffuse, 1.0);
    else
        gl_FragColor = lighting(pos, normal, diffuse, specular.xyz, specular.w);
}
