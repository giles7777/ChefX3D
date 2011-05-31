/** Demo of using gbuffer rendering pass. Vertex shader */

#version 110

attribute vec4 tangent;

varying vec3 vnormal;
varying vec3 vtangent;
varying vec3 vbinormal;

void main(){
    // Vertex position in object space
    gl_Position = ftransform();

    // Texture coordinates are just copied
    gl_TexCoord[0] = gl_MultiTexCoord0;

    // Calculate the binormal from the tangent and normal
    vec3 binormal = tangent.w * cross(gl_Normal, tangent.xyz);

    vec4 tmp = vec4(gl_Normal.xyz, 0);
    vnormal = (gl_ModelViewMatrix * tmp).xyz;

    tmp = vec4(tangent.xyz, 0);
    vtangent = (gl_ModelViewMatrix * tmp).xyz;

    tmp = vec4(binormal, 0);
    vbinormal = (gl_ModelViewMatrix * tmp).xyz;
}
