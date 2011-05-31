/** Demo of using an bloom blur pass in a deferred shading system. 
Fragment shader */

#version 110

#define NUM_SAMPLES 5

uniform sampler2D colourMap;
uniform vec2 texSize;

void main()
{
    vec3 gaussianCoeff[NUM_SAMPLES];
    gaussianCoeff[0] = vec3(0.00390625);
    gaussianCoeff[1] = vec3(0.03125);
    gaussianCoeff[2] = vec3(0.109375);
    gaussianCoeff[3] = vec3(0.21875);
    gaussianCoeff[4] = vec3(0.2734375);

    // Total value of all sampled pixels
    vec3 sum = vec3(0.0);

    vec2 offset = texSize * -2.0;

    for(int i = 0; i < NUM_SAMPLES; i++) 
    {
        vec3 sample = texture2D(colourMap, 
                                gl_TexCoord[0].st + offset).rgb;
        
        sum += sample * gaussianCoeff[i];

        // Head to the next pixel
        offset += texSize;
    }

    gl_FragColor = vec4(sum, 1.0);
}
