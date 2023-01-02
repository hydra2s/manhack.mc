package org.hydra2s.manhack;

//
import org.hydra2s.manhack.collector.GlDrawCollector;
import org.hydra2s.manhack.shared.vulkan.GlVulkanSharedBuffer;
import org.hydra2s.noire.descriptors.*;
import org.hydra2s.noire.objects.*;
import org.hydra2s.utils.Generator;
import org.hydra2s.utils.Promise;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

//
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.function.Function;

//
import static org.lwjgl.opengl.EXTSemaphore.*;
import static org.lwjgl.opengl.EXTSemaphoreWin32.GL_HANDLE_TYPE_OPAQUE_WIN32_EXT;
import static org.lwjgl.opengl.EXTSemaphoreWin32.glImportSemaphoreWin32HandleEXT;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR;
import static org.lwjgl.vulkan.VK10.*;

// the main rendering class!!!
// TODO: finish up projecting
public class GlRendererObj extends BasicObj {
    public PhysicalDeviceObj physicalDevice;
    public InstanceObj instance;
    public DeviceObj logicalDevice;
    public MemoryAllocatorObj memoryAllocator;
    public PipelineLayoutObj pipelineLayout;
    public WindowObj window;
    public SwapChainObj swapchain;
    public Generator<Integer> processor;

    public LongBuffer fences;
    public ArrayList<Promise<Integer>> promises = new ArrayList<Promise<Integer>>();
    public ArrayList<VkCommandBuffer> commandBuffers = new ArrayList<VkCommandBuffer>();
    public Iterator<Integer> process;
    //public PipelineObj.ComputePipelineObj finalComp;
    //public PipelineObj.GraphicsPipelineObj trianglePipeline;
    public ImageSetCInfo.FBLayout fbLayout;
    public ImageSetObj.FramebufferObj framebuffer;
    //public AccelerationStructureObj.TopAccelerationStructureObj topLvl;
    //public AccelerationStructureObj.BottomAccelerationStructureObj bottomLvl;

    //
    public int glSignalSemaphore = 0;
    public int glWaitSemaphore = 0;

    //
    public GlRendererObj initializer() throws IOException {
        InstanceObj.globalHandleMap.put((this.handle = new Handle("Renderer", MemoryUtil.memAddress(memAllocLong(1)))).get(), this);

        //
        var instanceCInfo = new InstanceCInfo();
        this.instance = new InstanceObj(null, instanceCInfo);

        //
        var physicalDevices = instance.enumeratePhysicalDevicesObj();
        this.physicalDevice = physicalDevices.get(0);

        //
        var queueCInfo = new DeviceCInfo.QueueFamilyCInfo() {{
            index = 0;
            priorities = new float[]{1.0F};
        }};

        //
        this.logicalDevice = new DeviceObj(physicalDevice.getHandle(), new DeviceCInfo() {{
            queueFamilies.add(queueCInfo);
        }});
        this.memoryAllocator = new MemoryAllocatorObj(logicalDevice.getHandle(), new MemoryAllocatorCInfo(){{}});



        //
        var _memoryAllocator = this.memoryAllocator;
        this.pipelineLayout = new PipelineLayoutObj(logicalDevice.getHandle(), new PipelineLayoutCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
        }});



        //
        return this;
    }

    //
    public GlRendererObj submitOnce(Function<VkCommandBuffer, Integer> fx) {
        this.logicalDevice.submitOnce(logicalDevice.getCommandPool(0), new BasicCInfo.SubmitCmd(){{
            queue = logicalDevice.getQueue(0, 0);

        }}, fx);
        return this;
    }

    //
    public GlRendererObj pipelines() throws IOException {
        //var finalCompSpv = Files.readAllBytes(Path.of("./shaders/final.comp.spv"));
        var _pipelineLayout = this.pipelineLayout;
        var _memoryAllocator = memoryAllocator;

        //
        this.fbLayout = new ImageSetCInfo.FBLayout(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            pipelineLayout = _pipelineLayout.getHandle().get();

            extents = new ArrayList<>(){{
                add(VkExtent3D.calloc().width(1280).height(720).depth(1));
            }};
            formats = memAllocInt(1).put(0, VK_FORMAT_R32G32B32A32_SFLOAT);
            layerCounts = new ArrayList<>(){{
                add(1);
            }};

            //
            blendAttachments = VkPipelineColorBlendAttachmentState.calloc(1);
            blendAttachments.get(0)
                .blendEnable(false)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);

            //
            attachmentInfos = VkRenderingAttachmentInfo.calloc(1);
            attachmentInfos.get(0)
                .clearValue(VkClearValue.calloc().color(VkClearColorValue.calloc()
                    .float32(memAllocFloat(4)
                        .put(0, 0.0F)
                        .put(1, 0.0F)
                        .put(2, 0.0F)
                        .put(3, 1.0F)
                    )));

            // TODO: support only depth or only stencil
            depthStencilAttachmentInfo = VkRenderingAttachmentInfo.calloc()
                .clearValue(VkClearValue.calloc().depthStencil(VkClearDepthStencilValue.calloc()
                    .depth(1.0F)
                    .stencil(0)));

            // TODO: support only depth or only stencil
            depthStencilFormat = VK_FORMAT_D32_SFLOAT_S8_UINT;
            scissor = VkRect2D.calloc()
                .extent(VkExtent2D.calloc().width(1280).height(720))
                .offset(VkOffset2D.calloc().x(0).y(0));
            viewport = VkViewport.calloc()
                .x(0.F).y(0.F)
                .width(1280.F).height(720.F)
                .minDepth(0.F).maxDepth(1.F);
        }};

        //
        this.framebuffer = new ImageSetObj.FramebufferObj(this.logicalDevice.getHandle(), this.fbLayout);

        //
        var _fbLayout = this.fbLayout;

        //
        //this.finalComp = new PipelineObj.ComputePipelineObj(logicalDevice.getHandle(), new PipelineCInfo.ComputePipelineCInfo(){{
            //pipelineLayout = _pipelineLayout.getHandle().get();
            //computeCode = memAlloc(finalCompSpv.length).put(0, finalCompSpv);
        //}});

        //
        //var fragSpv = Files.readAllBytes(Path.of("./shaders/triangle.frag.spv"));
        //var vertSpv = Files.readAllBytes(Path.of("./shaders/triangle.vert.spv"));
        //this.trianglePipeline = new PipelineObj.GraphicsPipelineObj(logicalDevice.getHandle(), new PipelineCInfo.GraphicsPipelineCInfo(){{
            //pipelineLayout = _pipelineLayout.getHandle().get();
            //fbLayout = _fbLayout;
            //sourceMap = new HashMap<>(){{
                //put(VK_SHADER_STAGE_FRAGMENT_BIT, memAlloc(fragSpv.length).put(0, fragSpv));
                //put(VK_SHADER_STAGE_VERTEX_BIT, memAlloc(vertSpv.length).put(0, vertSpv));
            //}};

        //}});

        return this;
    }

    // TODO: merge from `GLVulkanSharedBuffer`
    public GlRendererObj acceleration() {
        var _pipelineLayout = this.pipelineLayout;
        var _memoryAllocator = memoryAllocator;
        return this;
    }

    //
    public GlRendererObj tickRendering () {
        this.logicalDevice.doPolling();

        // TODO: available only when fully replace to Vulkan API!
        // Due that is dedicated thread
        /*
        if (this.process != null && this.process.hasNext()) {
            this.process.next();
        } else {
            if (this.process != null) { this.process = null; };
            this.process = this.generate().iterator();
        }*/
        this.generate();

        //
        return this;
    };

    //public Generator<Integer> generate() {
    public void generate() {

        // Wait a OpenGL signal to Vulkan...
        glSignalSemaphoreEXT(glSignalSemaphore, memAllocInt(0), memAllocInt(0), memAllocInt(0));
        var imageIndex = swapchain.acquireImageIndex(swapchain.semaphoreImageAvailable.getHandle().get());
        var promise = promises.get(imageIndex);

        // crap operation...
        do {
            if (promise.state().equals(Future.State.RUNNING)) {
                //this.yield(VK_NOT_READY);
            }
        } while(!promise.state().equals(Future.State.RUNNING));

        //
        var _queue = logicalDevice.getQueue(0, 0);
        
        //
        GlDrawCollector.buildDraw();
        this.submitOnce((cmdBuf)->{
            GlVulkanSharedBuffer.bottomLvl.cmdBuild(cmdBuf, GlVulkanSharedBuffer.drawRanges, VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);
            GlVulkanSharedBuffer.instanceBuffer.cmdSynchronizeFromHost(cmdBuf);
            GlVulkanSharedBuffer.topLvl.cmdBuild(cmdBuf, VkAccelerationStructureBuildRangeInfoKHR.calloc(1)
                        .primitiveCount(1)
                        .firstVertex(0)
                        .primitiveOffset(0)
                        .transformOffset(0),
                VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);

            return VK_SUCCESS;
        });

        // TODO: dynamic commanding
        logicalDevice.submitCommand(new BasicCInfo.SubmitCmd(){{
            queue = _queue;
            cmdBuf = commandBuffers.get(imageIndex);
            onDone = promises.get(imageIndex);
        }});

        //
        promises.set(imageIndex, new Promise<>());

        // TODO: use in glContext
        // TODO: bind with swapchain images
        // Wait a Vulkan, signal to OpenGL
        swapchain.present(_queue, memLongBuffer(memAddress(swapchain.semaphoreRenderingAvailable.getHandle().ptr(), 0), 1));
        glWaitSemaphoreEXT(glWaitSemaphore, memAllocInt(0), memAllocInt(0), memAllocInt(0));
        //System.out.println("GL semaphore is probably broken...");

        // TODO: available only when fully replace to Vulkan API...
        /*return (this.processor = new Generator<Integer>() {
            @Override
            protected void run() throws InterruptedException {
            }
        });*/
    }

    //
    public GlRendererObj rendering() {

        for (var I=0;I<this.swapchain.getImageCount();I++) {
            var cmdBuf = this.logicalDevice.allocateCommand(this.logicalDevice.getCommandPool(0));
            var pushConst = memAllocInt(4);
            pushConst.put(0, swapchain.getImageView(I).DSC_ID);
            pushConst.put(1, framebuffer.writingImageViews.get(0).DSC_ID);

            //
            if (GlVulkanSharedBuffer.topLvl != null) {
                memLongBuffer(memAddress(pushConst, 2), 1).put(0, GlVulkanSharedBuffer.topLvl.getDeviceAddress());
            }

            int finalI = I;
            this.logicalDevice.writeCommand(cmdBuf, (_cmdBuf_)->{
                //this.trianglePipeline.cmdDraw(cmdBuf, VkMultiDrawInfoEXT.calloc(1).put(0, VkMultiDrawInfoEXT.calloc().vertexCount(3).firstVertex(0)), this.framebuffer.getHandle().get(), memByteBuffer(pushConst), 0);
                //this.finalComp.cmdDispatch(cmdBuf, VkExtent3D.calloc().width(1280/32).height(720/6).depth(1), memByteBuffer(pushConst), 0);

                // FOR TEST ONLY!
                vkCmdClearColorImage(cmdBuf, this.swapchain.getImage(finalI), VK_IMAGE_LAYOUT_GENERAL, VkClearColorValue.calloc().float32(memAllocFloat(4).put(0, 1.F).put(1, 0.F).put(2, 0.F).put(3, 0.F)), VkImageSubresourceRange.calloc().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).layerCount(1).levelCount(1));
                return VK_SUCCESS;
            });

            this.commandBuffers.add(cmdBuf);
        }

        return this;
    }

    //
    public GlRendererObj prepare() {

        return this;
    }

    //
    public GlRendererObj windowed() {
        var _pipelineLayout = pipelineLayout;
        var _memoryAllocator = memoryAllocator;

        //
        /*this.window = new WindowObj(this.instance.handle, new WindowCInfo(){{
            size = VkExtent2D.calloc().width(1280).height(720);
            pipelineLayout = _pipelineLayout.handle.get();
        }});*/

        //
        this.swapchain = new SwapChainObj.SwapChainVirtual(this.logicalDevice.getHandle(), new SwapChainCInfo.VirtualSwapChainCInfo(){{
            pipelineLayout = _pipelineLayout.getHandle().get();
            queueFamilyIndex = 0;
            memoryAllocator = _memoryAllocator.getHandle().get();
            extent = VkExtent2D.calloc().width(1280).height(720);
        }});

        //
        glImportSemaphoreWin32HandleEXT(this.glSignalSemaphore = glGenSemaphoresEXT(), GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, swapchain.semaphoreImageAvailable.Win32Handle.get(0));
        glImportSemaphoreWin32HandleEXT(this.glWaitSemaphore = glGenSemaphoresEXT(), GL_HANDLE_TYPE_OPAQUE_WIN32_EXT, swapchain.semaphoreRenderingAvailable.Win32Handle.get(0));

        //
        this.fences = memAllocLong(this.swapchain.getImageCount());
        this.promises = new ArrayList<Promise<Integer>>();

        // EXAMPLE!
        for (var I=0;I<fences.remaining();I++) {
            vkCreateFence(logicalDevice.device, VkFenceCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).flags(VK_FENCE_CREATE_SIGNALED_BIT), null, fences.slice(I, 1));
            this.promises.add(new Promise<Integer>());
        }

        //
        this.submitOnce((cmdBuf)->{
            this.swapchain.imageViews.forEach((img)->{
                img.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_GENERAL, true);
            });
            this.framebuffer.processCurrentImageViews((img)->{
                img.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, true);
                return 0;
            });
            this.framebuffer.processWritingImageViews((img)->{
                img.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_GENERAL, true);
                return 0;
            });
            this.framebuffer.currentDepthStencilImageView.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, true);
            return 0;
        });

        //
        return this;
    }

    //
    public GlRendererObj(Handle base, Handle handle) throws IOException {
        super(base, handle);

        this.initializer();
        this.pipelines();
        this.windowed();
        this.prepare();
        this.acceleration();
        this.rendering();
    }

    //
    public GlRendererObj(Handle base, RendererCInfo cInfo) throws IOException {
        super(base, cInfo);

        this.initializer();
        this.pipelines();
        this.windowed();
        this.prepare();
        this.acceleration();
        this.rendering();
    }

}
