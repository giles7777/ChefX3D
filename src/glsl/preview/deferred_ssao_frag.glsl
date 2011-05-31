/** Demo of using an Anti-aliasing blur pass based on edge detection
in a deferred shading system. Fragment shader */

#version 110

uniform sampler2D randomNoiseMap;

uniform sampler2D normalMap;

uniform sampler2D depthMap;

// Global strength multiplier for the effect
uniform float globalStrength;

// Multiplier that can be used to tweak the strength of the occlusion.
// Combines with falloff to give amount of effect
uniform float strength;

// The sample radius to use
uniform float sampleRadius;

// An offset in to the random noise texture
uniform float offset;

// A falloff factor from the current sample
uniform float falloff;

// The number of samples to take in the samples[] array
const int NUM_SAMPLES = 10;

void main()
{
    vec3 samples[NUM_SAMPLES];
    samples[0] = vec3(-0.010735935, 0.01647018, 0.0062425877);
    samples[1] = vec3(-0.06533369, 0.3647007, -0.13746321);
    samples[2] = vec3(-0.6539235, -0.016726388, -0.53000957);
    samples[3] = vec3(0.40958285, 0.0052428036, -0.5591124);
    samples[4] = vec3(-0.1465366, 0.09899267, 0.15571679);
    samples[5] = vec3(-0.44122112, -0.5458797, 0.04912532);
    samples[6] = vec3(0.03755566, -0.10961345, -0.33040273);
    samples[7] = vec3(0.019100213, 0.29652783, 0.066237666);
    samples[8] = vec3(0.8765323, 0.011236004, 0.28265962);
    samples[9] = vec3(0.29264435, -0.40794238, 0.15964167);

    vec3 fres = normalize((texture2D(randomNoiseMap, gl_TexCoord[0].st * offset).rgb * 2.0) - 1.0);

    vec3 pixel_normal = texture2D(normalMap, gl_TexCoord[0].st).rgb;
    float pixel_depth = length(texture2D(depthMap, gl_TexCoord[0].st));

    vec3 ss_coords = vec3(gl_TexCoord[0].st, pixel_depth);

    // adjust for the depth 
    float scaled_rad = sampleRadius / pixel_depth;

    float occlusion = 0.0;

    for(int i = 0; i < NUM_SAMPLES; i++)
    {
        vec3 ray = scaled_rad * reflect(samples[i], fres);

        vec2 sample_coord = ss_coords.xy + sign(dot(ray, pixel_normal)) * ray.xy;

        vec3 occluder_normal = texture2D(normalMap, sample_coord).rgb;
        float occluder_depth = length(texture2D(depthMap, sample_coord));

        // Depth of the occluder fragment

        // Check depth difference. If depth is -ve, occluder is behind current frag
        float depth_diff = pixel_depth - occluder_depth;

        // calculate the difference between the normals as a weight
        // the falloff equation, starts at falloff and is kind of 1/x^2 falling
        occlusion += step(falloff, depth_diff) * 
                    (1.0 - dot(occluder_normal, pixel_normal)) *
                    (1.0 - smoothstep(falloff, strength, depth_diff));
    }

    occlusion = 1.0 + occlusion * -globalStrength / float(NUM_SAMPLES);

    gl_FragColor = vec4(occlusion, occlusion, occlusion, 1.0);
}
