/** Demo of using gbuffer rendering pass. Fragment shader */

#version 110

uniform sampler2D normalMap;

varying vec3 vnormal;
varying vec3 vtangent;
varying vec3 vbinormal;

void main(){
	if (gl_FrontMaterial.diffuse.a == 0.0) {
		// totally transparent, don't bother...
		discard;

	} else {
	    // look up the normal map
	    vec3 normal = texture2D(normalMap, gl_TexCoord[0].xy).xyz;
	
	    // transform the normal to view space
	    normal -= 0.5;
	    normal = normalize(normal.x * vtangent + 
	                       normal.y * vbinormal +
	               normal.z * vnormal);
	
	    // Now back to colour space of [0, 1]
	    normal = normal * 0.5 + 0.5;
	
	    gl_FragData[0] = vec4(normal, 1.0);
	    gl_FragData[1] = vec4(1.0, 0.0, 0.0, 1.0);
	}
}
