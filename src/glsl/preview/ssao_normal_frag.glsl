/** Demo of using gbuffer rendering pass. Fragment shader */

#version 110

// stuff for ssao in eye space coords
varying vec3 esNormal;
varying float esDepth; 
varying float transparency;

void main(){
    if(transparency < 0.95)
        discard;

    gl_FragColor = vec4(normalize(esNormal), 1.0);
    gl_FragDepth = esDepth;
}
