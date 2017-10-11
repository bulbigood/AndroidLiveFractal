precision highp float;

varying vec2 v_texCoord;

uniform vec2 texSize;
uniform vec2 offset_coord;

uniform vec3 pos;
uniform vec3 prev_pos;

uniform sampler2D prev_frame;

//-------------------

uniform int iterationsNum;

uniform vec4 fractal_color;
uniform vec4 back_color;

uniform vec4 colors[10];
uniform float alternation_colors_num;

uniform sampler2D color_gradient;

//-------------------

uniform float antiflickering_new_pixel_weight;
uniform vec2 edge_detection_step;

uniform int color_mode;
uniform int antiflickering_mode;
uniform int edge_detection_mode;

vec4 computeMandelbrotPixel(vec2 coord){
        vec4 pixel = vec4(0.0);

        int iter = 0;
		vec2 z = vec2 (0.0);
		vec2 z2 = vec2 (0.0);
		float tmp = 0.0;

		int iterations = iterationsNum + 1;
		while (z2.x + z2.y < 4.0 && iter < iterations)
		{
			tmp = z2.x - z2.y + coord.x;
			z.y = 2.0*z.x*z.y + coord.y;
			z.x = tmp;
			z2 = z*z;

			iter++;
		}
		iter--;

    if(iter == 0){
            pixel = back_color;
    } else if(iter == iterationsNum) {
            pixel = fractal_color;
    } else if(color_mode == 0){
			//Однородный цвет
			pixel = colors[0];
	} else if (color_mode == 1){
			//Чередование цветов
			pixel = colors[int(mod(float(iter), alternation_colors_num))];
	} else if (color_mode == 2){
			//Градиент
			vec2 grad_coord = vec2(float(iter) / float(iterationsNum), 0.0);
            pixel = texture2D(color_gradient, grad_coord);
	}
	return pixel;
}

void main()
{
	vec2 texCoord = v_texCoord * texSize;

	vec2 offset = texCoord + offset_coord;
    if(offset.x >= 0.0 && offset.y >= 0.0 && offset.x < texSize.x && offset.y < texSize.y)
        discard;

	float step = 1.0 / pos.z;
	vec2 coord = texCoord * step + pos.xy;

    bool inserted = false;
	vec4 pixel = vec4(-1.0);
	float minimalDistance = 0.0;

    if(antiflickering_mode == 1 || edge_detection_mode == 1){
	    vec2 diff = coord - prev_pos.xy;
        if(diff.x > 0.0 && diff.y > 0.0 && diff.x < texSize.x * step && diff.y < texSize.y * step){
    	    vec2 oldIndex = diff * prev_pos.z / texSize;
            vec4 center = texture2D(prev_frame, oldIndex);

            vec2 rt_index = oldIndex + edge_detection_step;
            vec2 lb_index = oldIndex - edge_detection_step;
            if(edge_detection_mode == 1 && rt_index.x <= 1.0 && rt_index.y <= 1.0 && lb_index.x >= 0.0 && lb_index.y >= 0.0){
                vec4 rt = texture2D(prev_frame, rt_index);
                vec4 lb = texture2D(prev_frame, lb_index);
                vec4 edge = abs(2.0*center - rt - lb);

    	        if(edge.r + edge.g + edge.b < 0.001){
                    pixel = center;
                }
            }

            if(antiflickering_mode == 1 && pixel.r == -1.0){
                minimalDistance = distance(coord, floor(coord * prev_pos.z + 0.5) / prev_pos.z);
                if(minimalDistance < step){
                    pixel = center;
                    inserted = true;
                }
            }
	    }
	}

	if(pixel.r != -1.0){
        if(antiflickering_mode == 1 && inserted){
            float weight1 = antiflickering_new_pixel_weight;
            float weight2 = (step - minimalDistance) / step;
            gl_FragColor = (computeMandelbrotPixel(coord) * weight1 + pixel * weight2) / (weight1 + weight2);
        } else {
            gl_FragColor = pixel;
        }
    } else {
	    gl_FragColor = computeMandelbrotPixel(coord);
	}
}