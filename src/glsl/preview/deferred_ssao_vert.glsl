/** Demo of using an Anti-aliasing blur pass based on edge detection
in a deferred shading system. Vertex shader */

#version 110

void main(){
    // Vertex position in object space
    gl_Position = ftransform();
    gl_TexCoord[0] = gl_MultiTexCoord0;
}
