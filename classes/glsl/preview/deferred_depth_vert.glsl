/** Depth pass of the gbuffer rendering passes. Vertex shader */

#version 110

uniform float grow;

varying float distance;

void main()
{
  vec4 pos = gl_Vertex;
  pos.xyz += gl_Normal * grow;  // scale vertex along normal
  gl_Position = gl_ModelViewProjectionMatrix * pos;
  distance = length(gl_ModelViewMatrix * gl_Vertex);
}
