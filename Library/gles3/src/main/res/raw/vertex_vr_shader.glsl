attribute vec4 aPosition;// 顶点位置
attribute vec2 aTexCoord;// S T 纹理坐标
varying vec2 vTexCoord;

uniform mat4 uProjMatrix;
uniform mat4 uRotateMatrix;
uniform mat4 uViewMatrix;
uniform mat4 uModelMatrix;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = uProjMatrix * uRotateMatrix * uViewMatrix * uModelMatrix * aPosition;
}
