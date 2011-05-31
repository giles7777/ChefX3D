/** Depth pass of the gbuffer rendering passes. Frag shader */

#version 110

varying float distance;

void main(){
    gl_FragColor = vec4(0, 0, 0, 0);
    gl_FragDepth = distance;
}
