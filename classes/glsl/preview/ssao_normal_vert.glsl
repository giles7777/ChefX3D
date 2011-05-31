/** Demo of using gbuffer rendering pass. Vertex shader */

#version 110

uniform float depthScale;

// stuff for ssao in eye space
varying vec3 esNormal;
varying float esDepth; 
varying float transparency;

void main(){
    // Vertex position in object space
    gl_Position = ftransform();

    // Texture coordinates are just copied
    gl_TexCoord[0] = gl_MultiTexCoord0;

    transparency = gl_FrontMaterial.diffuse.a;

    // ssao 
    vec4 vps = gl_ModelViewMatrix * gl_Vertex;
    esDepth = -vps.z * depthScale;
    esNormal = normalize((gl_ModelViewMatrix * vec4(gl_Normal.xyz, 0.0)).xyz);
}
