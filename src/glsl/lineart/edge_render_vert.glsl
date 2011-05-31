/** Demo of using the light pass in a deferred shading system. Vertex shader */

#version 110

void main(){
    // Vertex position in object space
    gl_Position = ftransform();

    // Texture coordinates are just copied
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
