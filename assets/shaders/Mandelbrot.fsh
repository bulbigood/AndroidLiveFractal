precision highp float;

varying vec2 v_texCoord;

uniform float texSize;
uniform vec2 screen;
uniform float quality;

uniform vec3 pos;
uniform mat2 rotationMatrix;
uniform vec3 colors;
uniform int iterationsNum;

void main()
{
	vec2 texCoord = v_texCoord * texSize * quality;

	if(texCoord.x > screen.x*quality || texCoord.y > screen.y*quality)
		discard;

	vec2 coord = texCoord / (pos.z * quality);
    coord *= rotationMatrix;
    coord += pos.xy;

	int iter = iterationsNum;
	vec2 z = vec2 (0.0);
	vec2 z2 = vec2 (0.0);
	float tmp = 0.0;

	while (z2.x + z2.y < 4.0 && --iter > 1)
	{
		tmp = z2.x - z2.y + coord.x;
		z.y = 2.0*z.x*z.y + coord.y;
		z.x = tmp;
		z2 = z*z;
	}

	gl_FragColor = vec4 (mod(colors * float(iter), 0.8), 1.0);
}