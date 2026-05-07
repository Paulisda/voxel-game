package de.paul.voxelgame.core;

import de.paul.voxelgame.assets.ResourceManager;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;

public final class TextureLoader {
    private final ResourceManager resources;

    public TextureLoader() {
        this(new ResourceManager());
    }

    public TextureLoader(final ResourceManager resources) {
        this.resources = resources;
    }

    public int loadTexture(final String path) {
        try (InputStream input = resources.open(path)) {
            final BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported image resource: " + path);
            }
            return uploadTexture(image);
        } catch (IOException e) {
            throw new IllegalArgumentException("Texture not readable: " + path, e);
        }
    }

    public int loadTexture(final byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Texture data must not be empty");
        }
        try {
            final BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new IllegalArgumentException("Unsupported image data");
            }
            return uploadTexture(image);
        } catch (IOException e) {
            throw new IllegalArgumentException("Texture data not readable", e);
        }
    }

    public int loadSolidTexture(final int rgba) {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        buffer.put((byte) ((rgba >>> 24) & 0xff));
        buffer.put((byte) ((rgba >>> 16) & 0xff));
        buffer.put((byte) ((rgba >>> 8) & 0xff));
        buffer.put((byte) (rgba & 0xff));
        buffer.flip();
        return uploadTexture(1, 1, buffer);
    }

    public int uploadTexture(final BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);

        final ByteBuffer rgba = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                final int pixel = argb[y * width + x];
                rgba.put((byte) ((pixel >>> 16) & 0xff));
                rgba.put((byte) ((pixel >>> 8) & 0xff));
                rgba.put((byte) (pixel & 0xff));
                rgba.put((byte) ((pixel >>> 24) & 0xff));
            }
        }
        rgba.flip();

        return uploadTexture(width, height, rgba);
    }

    public int uploadTexture(final int width, final int height, final ByteBuffer pixels) {
        final int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        return textureId;
    }

    public void deleteTexture(final int textureId) {
        if (textureId > 0) {
            glDeleteTextures(textureId);
        }
    }
}
