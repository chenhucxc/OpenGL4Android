attribute vec4 vPosition;
attribute vec2 a_texCoord;
varying vec2 v_texCoord;

uniform mat4 uRotateMatrix;
uniform mat4 uMVPMatrix;

void main() {
    gl_Position = uMVPMatrix * vPosition;
    v_texCoord = a_texCoord;
}
