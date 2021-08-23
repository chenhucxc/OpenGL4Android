#version 300 es
// 声明着色器中浮点变量的默认精度
precision mediump float;
// 声明一个输入名为vColor的4分向量，来自上面的顶点着色器（fragment_simple_shade.glsl）
in vec4 vColor;
// 着色器声明一个输出变量fragColor，这个是一个4分量的向量
out vec4 fragColor;
void main() {
    // 表示将输入的颜色值数据拷贝到fragColor变量中，输出到颜色缓冲区
    fragColor = vColor;
}


