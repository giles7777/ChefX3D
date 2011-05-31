/** Demo of using the light pass in a deferred shading system. Vertex shader */

#version 110

attribute vec3 viewCoords;

varying vec3 viewCoordsPosition;
varying vec3 viewSpacePosition;

void main(){
    viewCoordsPosition = viewCoords;
    viewSpacePosition = (gl_ModelViewMatrix * gl_Vertex).xyz;

    // Vertex position in object space
    gl_Position = ftransform();

    // Texture coordinates are just copied
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
