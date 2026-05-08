package de.paul.voxelgame.renderer;

import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.core.TextureLoader;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex2f;

public final class TextRenderer {
    private static final Font BASE_FONT = loadBaseFont();

    private final TextureLoader textureLoader = new TextureLoader();
    private final Map<TextKey, TextTexture> cache = new HashMap<>();

    public void drawText(final String text, final float x, final float y, final int size, final Color color) {
        drawText(text, x, y, size, color, 1.0f);
    }

    public void drawText(final String text, final float x, final float y, final int size, final Color color, final float alpha) {
        final TextTexture texture = texture(text, size, color, Font.PLAIN);
        draw(texture, x, y, alpha);
    }

    public void drawBoldText(final String text, final float x, final float y, final int size, final Color color) {
        final TextTexture texture = texture(text, size, color, Font.BOLD);
        draw(texture, x, y);
    }

    public void drawCenteredText(final String text, final float centerX, final float y, final int size, final Color color) {
        drawCenteredText(text, centerX, y, size, color, 1.0f);
    }

    public void drawCenteredText(final String text, final float centerX, final float y, final int size, final Color color, final float alpha) {
        final TextTexture texture = texture(text, size, color, Font.PLAIN);
        draw(texture, centerX - texture.width() * 0.5f, y, alpha);
    }

    public void drawCenteredBoldText(final String text, final float centerX, final float y, final int size, final Color color) {
        final TextTexture texture = texture(text, size, color, Font.BOLD);
        draw(texture, centerX - texture.width() * 0.5f, y);
    }

    public void destroy() {
        for (final TextTexture texture : cache.values()) {
            glDeleteTextures(texture.textureId());
        }
        cache.clear();
    }

    private void draw(final TextTexture texture, final float x, final float y) {
        draw(texture, x, y, 1.0f);
    }

    private void draw(final TextTexture texture, final float x, final float y, final float alpha) {
        glColor4f(1.0f, 1.0f, 1.0f, clampAlpha(alpha));
        glBindTexture(GL_TEXTURE_2D, texture.textureId());
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f);
        glVertex2f(x, y);
        glTexCoord2f(1.0f, 1.0f);
        glVertex2f(x + texture.width(), y);
        glTexCoord2f(1.0f, 0.0f);
        glVertex2f(x + texture.width(), y + texture.height());
        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(x, y + texture.height());
        glEnd();
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private float clampAlpha(final float alpha) {
        return Math.max(0.0f, Math.min(1.0f, alpha));
    }

    private TextTexture texture(final String rawText, final int size, final Color color, final int style) {
        final String text = rawText == null || rawText.isBlank() ? " " : rawText;
        final int rgba = color.getRGB();
        final TextKey key = new TextKey(text, size, rgba, style);
        final TextTexture cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        final Font font = BASE_FONT.deriveFont(style, (float) size);
        final BufferedImage measureImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D measureGraphics = measureImage.createGraphics();
        measureGraphics.setFont(font);
        final FontMetrics metrics = measureGraphics.getFontMetrics();
        final int width = Math.max(1, metrics.stringWidth(text) + 4);
        final int height = Math.max(1, metrics.getHeight() + 4);
        measureGraphics.dispose();

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(font);
        graphics.setColor(color);
        graphics.drawString(text, 2, 2 + metrics.getAscent());
        graphics.dispose();

        final TextTexture texture = new TextTexture(textureLoader.uploadTexture(image), width, height);
        cache.put(key, texture);
        return texture;
    }

    private static Font loadBaseFont() {
        final byte[] data = new ResourcePackLoader().loadFont("font");
        if (data != null && data.length > 0) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(data));
            } catch (FontFormatException | IOException ignored) {
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    private record TextKey(String text, int size, int rgba, int style) {
    }

    private record TextTexture(int textureId, int width, int height) {
    }
}
