/** Demo of using gbuffer rendering pass. Vertex shader */

#version 110

attribute vec4 tangent;

varying vec3 vpos;
varying vec3 vnormal;
varying vec3 vtangent;
varying vec3 vbinormal;
varying float transparency;

void main(){
    // Vertex position in object space
    gl_Position = ftransform();

    vpos = (gl_ModelViewMatrix * gl_Vertex).xyz;

    transparency = gl_FrontMaterial.diffuse.a;

    // Texture coordinates are just copied
    gl_TexCoord[0] = gl_MultiTexCoord0;

    // Calculate the binormal from the tangent and normal
    vec3 binormal = tangent.w * cross(gl_Normal, tangent.xyz);

    // Just the rotation part of the model view matrix
    mat3 model_view_rot = mat3(gl_ModelViewMatrix[0].xyz, 
                               gl_ModelViewMatrix[1].xyz, 
                               gl_ModelViewMatrix[2].xyz);

    vnormal = model_view_rot * gl_Normal;
    vtangent = model_view_rot * tangent.xyz;
    vbinormal = model_view_rot* binormal;
}
