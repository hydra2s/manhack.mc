glslangValidator --client vulkan100 --target-env spirv1.6 -o final.comp.spv final.comp
glslangValidator --client vulkan100 --target-env spirv1.6 -o triangle.frag.spv triangle.frag
glslangValidator --client vulkan100 --target-env spirv1.6 -o triangle.vert.spv triangle.vert
glslangValidator --client opengl100 --target-env spirv1.6 -o show.frag.spv show.frag
glslangValidator --client opengl100 --target-env spirv1.6 -o show.vert.spv show.vert
glslangValidator --client opengl100 --target-env spirv1.6 -o transform.vert.spv transform.vert
pause
