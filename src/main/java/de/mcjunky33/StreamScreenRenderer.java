package de.mcjunky33;

import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.Minecraft;
import org.joml.Matrix4fc;

import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;



public class StreamScreenRenderer {

    private static StreamScreenRenderer instance;

    public static StreamScreenRenderer getInstance() {
        return instance;
    }

    public void init() {
        instance = this;
        LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(this::onRender);
    }

    private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;

    private static final ByteBufferBuilder allocator = new ByteBufferBuilder(256);

    private BufferBuilder buffer;
    private MappableRingBuffer vertexBuffer;

    private void onRender(LevelRenderContext context) {
        if (!StreamScreen.isStreaming) return;

        TabManager.BrowserTab tab = BrowserScreen.getTabManager().getCurrent();
        if (tab == null || tab.browser == null || !tab.browser.isTextureReady()) return;

        renderStreamScreen(context);
        draw(Minecraft.getInstance(), PIPELINE, tab);
    }



    private void renderStreamScreen(LevelRenderContext context) {
        if (StreamScreen.pos1 == null || StreamScreen.pos2 == null) return;

        PoseStack stack = context.poseStack();
        Vec3 cam = context.levelState().cameraRenderState.pos;

        stack.pushPose();
        stack.translate(-cam.x, -cam.y, -cam.z);

        if (buffer == null) {
            buffer = new BufferBuilder(
                    allocator,
                    PIPELINE.getVertexFormatMode(),
                    PIPELINE.getVertexFormat()
            );
        }

        Matrix4fc mat = stack.last().pose();

        float x1 = Math.min(StreamScreen.pos1.getX(), StreamScreen.pos2.getX());
        float y1 = Math.min(StreamScreen.pos1.getY(), StreamScreen.pos2.getY());
        float z1 = Math.min(StreamScreen.pos1.getZ(), StreamScreen.pos2.getZ());

        float x2 = Math.max(StreamScreen.pos1.getX(), StreamScreen.pos2.getX()) + 1;
        float y2 = Math.max(StreamScreen.pos1.getY(), StreamScreen.pos2.getY()) + 1;
        float z2 = Math.max(StreamScreen.pos1.getZ(), StreamScreen.pos2.getZ()) + 1;

        float eps = 0.01f;

        Direction face = StreamScreen.face;

        switch (face) {

            case NORTH -> {
                float z = z1 - eps;

                buffer.addVertex(mat, x1, y1, z).setColor(0xFFFFFFFF).setUv(1f, 1f);
                buffer.addVertex(mat, x1, y2, z).setColor(0xFFFFFFFF).setUv(1f, 0f);
                buffer.addVertex(mat, x2, y2, z).setColor(0xFFFFFFFF).setUv(0f, 0f);
                buffer.addVertex(mat, x2, y1, z).setColor(0xFFFFFFFF).setUv(0f, 1f);
            }

            case SOUTH -> {
                float z = z2 + eps;

                buffer.addVertex(mat, x2, y1, z).setColor(0xFFFFFFFF).setUv(1f, 1f);
                buffer.addVertex(mat, x2, y2, z).setColor(0xFFFFFFFF).setUv(1f, 0f);
                buffer.addVertex(mat, x1, y2, z).setColor(0xFFFFFFFF).setUv(0f, 0f);
                buffer.addVertex(mat, x1, y1, z).setColor(0xFFFFFFFF).setUv(0f, 1f);

            }

            case WEST -> {
                float x = x1 - eps;

                buffer.addVertex(mat, x, y1, z2).setColor(0xFFFFFFFF).setUv(1f, 1f);
                buffer.addVertex(mat, x, y2, z2).setColor(0xFFFFFFFF).setUv(1f, 0f);
                buffer.addVertex(mat, x, y2, z1).setColor(0xFFFFFFFF).setUv(0f, 0f);
                buffer.addVertex(mat, x, y1, z1).setColor(0xFFFFFFFF).setUv(0f, 1f);
            }

            case EAST -> {
                float x = x2 + eps;

                buffer.addVertex(mat, x, y1, z1).setColor(0xFFFFFFFF).setUv(1f, 1f);
                buffer.addVertex(mat, x, y2, z1).setColor(0xFFFFFFFF).setUv(1f, 0f);
                buffer.addVertex(mat, x, y2, z2).setColor(0xFFFFFFFF).setUv(0f, 0f);
                buffer.addVertex(mat, x, y1, z2).setColor(0xFFFFFFFF).setUv(0f, 1f);
            }
        }

        stack.popPose();
    }


    private void draw(Minecraft client,
                      RenderPipeline pipeline,
                      TabManager.BrowserTab tab) {

        if (buffer == null || tab == null || tab.browser == null) return;

        MeshData mesh = buffer.buildOrThrow();
        MeshData.DrawState state = mesh.drawState();

        GpuBuffer vertices = upload(state, mesh);

        Identifier texId = tab.browser.getTextureIdentifier();

        var textureManager = Minecraft.getInstance().getTextureManager();
        var abstractTexture = textureManager.getTexture(texId);

        GpuTextureView view = abstractTexture.getTextureView();

        GpuSampler sampler = RenderSystem.getDevice().createSampler(
                com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
                com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
                com.mojang.blaze3d.textures.FilterMode.LINEAR,
                com.mojang.blaze3d.textures.FilterMode.LINEAR,
                1,
                OptionalDouble.empty()
        );

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> BrowsermodMCEFClient.MOD_ID + " stream screen",
                        client.getMainRenderTarget().getColorTextureView(),
                        OptionalInt.empty(),
                        client.getMainRenderTarget().getDepthTextureView(),
                        OptionalDouble.empty())) {

            pass.setPipeline(pipeline);
            pass.bindTexture("Sampler0", view, sampler);
            RenderSystem.bindDefaultUniforms(pass);

            pass.setVertexBuffer(0, vertices);

            pass.setIndexBuffer(
                    RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode())
                            .getBuffer(state.indexCount()),
                    RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode()).type()
            );

            pass.drawIndexed(0, 0, state.indexCount(), 1);
        }

        mesh.close();
        buffer = null;
    }



    private GpuBuffer upload(MeshData.DrawState state, MeshData mesh) {

        int size = state.vertexCount() * state.format().getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < size) {
            if (vertexBuffer != null) vertexBuffer.close();

            vertexBuffer = new MappableRingBuffer(
                    () -> BrowsermodMCEFClient.MOD_ID + " stream screen vb",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    size
            );
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView view = encoder.mapBuffer(
                vertexBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()),
                false, true)) {

            java.nio.ByteBuffer src = mesh.vertexBuffer();
            java.nio.ByteBuffer dst = view.data();

            dst.clear();
            dst.put(src);
            dst.flip();
        }

        return vertexBuffer.currentBuffer();
    }

    public void close() {
        allocator.close();

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}