#version 330 core

layout (location = 0) out vec4 color;

in vec2 v_TexCoord;
in vec4 v_Color;
in float v_TexIndex;

// Dynamic array length injected at runtime by the engine
uniform sampler2D u_Textures[#MAX_TEXTURE_SLOTS#];

void main() {
    int index = int(v_TexIndex);
    
    if (index == -1) {
        color = v_Color;
    } else {
        color = texture(u_Textures[index], v_TexCoord) * v_Color;
    }
}
