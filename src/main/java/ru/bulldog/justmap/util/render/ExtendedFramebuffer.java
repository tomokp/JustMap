package ru.bulldog.justmap.util.render;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.texture.TextureUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public class ExtendedFramebuffer extends Framebuffer {
	private int colorAttachment;
	private int depthAttachment;
	private Fbo type;
	
	public ExtendedFramebuffer(int width, int height, boolean useDepthIn) {
		super(width, height, useDepthIn, MinecraftClient.IS_SYSTEM_MAC);
	}

	public static boolean canUseFramebuffer() {
        return GL.getCapabilities().OpenGL14 && (
        		GL.getCapabilities().GL_ARB_framebuffer_object ||
        	    GL.getCapabilities().GL_EXT_framebuffer_object ||
        	    GL.getCapabilities().OpenGL30);
    }
	
	@Override
	public void resize(int width, int height, boolean isMac) {
		GlStateManager.enableDepthTest();
		if (fbo >= 0) {
			this.delete();
		}
		this.initFbo(width, height, isMac);
		this.checkFramebufferStatus();
		bindFramebuffer(type, GLC.GL_FRAMEBUFFER, 0);
	}
	
	@Override
	public void initFbo(int width, int height, boolean isMac) {
		this.viewportWidth = width;
		this.viewportHeight = height;
		this.textureWidth = width;
		this.textureHeight = height;
		this.fbo = this.genFrameBuffers();
		if (fbo == -1) {
			this.clear(isMac);
			return;
		}
		this.colorAttachment = TextureUtil.generateId();
		if (colorAttachment == -1) {
			this.clear(isMac);
			return;
		}
		if (useDepthAttachment) {
			this.depthAttachment = this.genRenderbuffers();
			if (depthAttachment == -1) {
				this.clear(isMac);
				return;
			}
		}
		this.setTexFilter(GLC.GL_NEAREST);
		GlStateManager.bindTexture(colorAttachment);
		GlStateManager.texImage2D(GLC.GL_TEXTURE_2D, 0, GLC.GL_RGBA8, textureWidth, textureHeight, 0, GLC.GL_RGBA, GLC.GL_UNSIGNED_BYTE, null);
		bindFramebuffer(type, GLC.GL_FRAMEBUFFER, fbo);
		framebufferTexture2D(type, GLC.GL_FRAMEBUFFER, GLC.GL_COLOR_ATTACHMENT, GLC.GL_TEXTURE_2D, colorAttachment, 0);
		if (useDepthAttachment) {
			bindRenderbuffer(type, GLC.GL_RENDERBUFFER, depthAttachment);
			renderbufferStorage(type, GLC.GL_RENDERBUFFER, GLC.GL_DEPTH_COMPONENT24, textureWidth, textureHeight);
			framebufferRenderbuffer(type, GLC.GL_FRAMEBUFFER, GLC.GL_DEPTH_ATTACHMENT, GLC.GL_RENDERBUFFER, depthAttachment);
		}
		this.clear(isMac);
		this.endRead();
	}
	
	private int genFrameBuffers() {
		int fbo = -1;
		this.type = Fbo.NONE;
		if (GL.getCapabilities().OpenGL30) {
			fbo = GL30.glGenFramebuffers();
			this.type = Fbo.BASE;
		}
		else if (GL.getCapabilities().GL_ARB_framebuffer_object) {
			fbo = ARBFramebufferObject.glGenFramebuffers();
			this.type = Fbo.ARB;
		}
		else if (GL.getCapabilities().GL_EXT_framebuffer_object) {
			fbo = EXTFramebufferObject.glGenFramebuffersEXT();
			this.type = Fbo.EXT;
		}
		return fbo;
	}
	
	public int genRenderbuffers() {
		switch (type) {
			case BASE: {
				return GL30.glGenRenderbuffers();
			}
			case ARB: {
				return ARBFramebufferObject.glGenRenderbuffers();
			}
			case EXT: {
				return EXTFramebufferObject.glGenRenderbuffersEXT();
			}
			default: {
				return -1;
			}
		}
	}
	
	public void delete() {
		this.endRead();
		this.endWrite();
		if (depthAttachment > -1) {
			this.deleteRenderbuffers(depthAttachment);
			this.depthAttachment = -1;
		}
		if (colorAttachment > -1) {
			TextureUtil.deleteId(colorAttachment);
			this.colorAttachment = -1;
		}
		if (fbo > -1) {
			bindFramebuffer(type, GLC.GL_FRAMEBUFFER, 0);
			this.deleteFramebuffers(fbo);
			this.fbo = -1;
		}
	}
	
	private void deleteFramebuffers(int framebufferIn) {
		switch (type) {
			case BASE: {
				GL30.glDeleteFramebuffers(framebufferIn);
				break;
			}
			case ARB: {
				ARBFramebufferObject.glDeleteFramebuffers(framebufferIn);
				break;
			}
			case EXT: {
				EXTFramebufferObject.glDeleteFramebuffersEXT(framebufferIn);
				break;
			}
			default: {}
		}
	}
	
	private void deleteRenderbuffers(int renderbuffer) {
		switch (type) {
			case BASE: {
				GL30.glDeleteRenderbuffers(renderbuffer);
				break;
			}
			case ARB: {
				ARBFramebufferObject.glDeleteRenderbuffers(renderbuffer);
				break;
			}
			case EXT: {
				EXTFramebufferObject.glDeleteRenderbuffersEXT(renderbuffer);
				break;
			}
			default: {}
		}
	}
	
	@Override
	public void checkFramebufferStatus() {
		int status = this.checkFramebufferStatus(GLC.GL_FRAMEBUFFER);
		switch (status) {
			case GLC.GL_FRAMEBUFFER_COMPLETE: {
				return;
			}
			case GLC.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT: {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
			}
			case GLC.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
			}
			case GLC.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER: {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
			}
			case GLC.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER: {
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
			}
			default: {
				throw new RuntimeException("glCheckFramebufferStatus returned unknown status: " + status);
			}
		}
	}
	
	private int checkFramebufferStatus(int target) {
		switch (type) {
			case BASE: {
				return GL30.glCheckFramebufferStatus(target);
			}
			case ARB: {
				return ARBFramebufferObject.glCheckFramebufferStatus(target);
			}
			case EXT: {
				return EXTFramebufferObject.glCheckFramebufferStatusEXT(target);
			}
			default: {
				return -1;
			}
		}
	}
	
	public static void bindFramebuffer(Fbo type, int target, int framebufferIn) {
		switch (type) {
			case BASE: {
				GL30.glBindFramebuffer(target, framebufferIn);
				break;
			}
			case ARB: {
				ARBFramebufferObject.glBindFramebuffer(target, framebufferIn);
				break;
			}
			case EXT: {
				EXTFramebufferObject.glBindFramebufferEXT(target, framebufferIn);
				break;
			}
			default: {
				throw new RuntimeException("bindFramebuffer: Invalid FBO type.");
			}
		}
	}
	
	public static void framebufferTexture2D(Fbo type, int target, int attachment, int textarget, int texture, int level) {
		switch (type) {
			case BASE: {
				GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
				break;
			}
			case ARB: {
				ARBFramebufferObject.glFramebufferTexture2D(target, attachment, textarget, texture, level);
				break;
			}
			case EXT: {
				EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
				break;
			}
			default: {
				throw new RuntimeException("framebufferTexture2D: Invalid FBO type.");
			}
		}
	}
	
	public static void bindRenderbuffer(Fbo type, int target, int renderbuffer) {
		switch (type) {
			case BASE: {
				GL30.glBindRenderbuffer(target, renderbuffer);
				break;
			}
			case ARB: {
				ARBFramebufferObject.glBindRenderbuffer(target, renderbuffer);
				break;
			}
			case EXT: {
				EXTFramebufferObject.glBindRenderbufferEXT(target, renderbuffer);
				break;
			}
			default: {
				throw new RuntimeException("bindRenderbuffer: Invalid FBO type.");
			}
		}
	}
	
	public static void renderbufferStorage(Fbo type, int target, int internalFormat, int width, int height) {
		switch (type) {
			case BASE: {
				GL30.glRenderbufferStorage(target, internalFormat, width, height);
				break;
			}
			case ARB: {
				ARBFramebufferObject.glRenderbufferStorage(target, internalFormat, width, height);
				break;
			}
			case EXT: {
				EXTFramebufferObject.glRenderbufferStorageEXT(target, internalFormat, width, height);
				break;
			}
			default: {
				throw new RuntimeException("renderbufferStorage: Invalid FBO type.");
			}
		}
	}
	
	public static void framebufferRenderbuffer(Fbo type, int target, int attachment, int renderBufferTarget, int renderBuffer) {
		switch (type) {
			case BASE: {
				GL30.glFramebufferRenderbuffer(target, attachment, renderBufferTarget, renderBuffer);
				break;
			}
			case ARB: {
				ARBFramebufferObject.glFramebufferRenderbuffer(target, attachment, renderBufferTarget, renderBuffer);
				break;
			}
			case EXT: {
				EXTFramebufferObject.glFramebufferRenderbufferEXT(target, attachment, renderBufferTarget, renderBuffer);
				break;
			}
			default: {
				throw new RuntimeException("framebufferRenderbuffer: Invalid FBO type.");
			}
		}
	}
	
	@Override
	public void beginWrite(boolean setViewport) {
		bindFramebuffer(type, GLC.GL_FRAMEBUFFER, fbo);
		if (setViewport) {
			GlStateManager.viewport(0, 0, viewportWidth, viewportHeight);
		}
	}
	
	@Override
	public void endWrite() {
		bindFramebuffer(type, GLC.GL_FRAMEBUFFER, 0);
	}
	
	@Override
	public void beginRead() {
		GlStateManager.bindTexture(colorAttachment);
	}
	
	@Override
	public void endRead() {
		GlStateManager.bindTexture(0);
	}
	
	@Override
	public void setTexFilter(int framebufferFilterIn) {
		this.texFilter = framebufferFilterIn;
		GlStateManager.bindTexture(colorAttachment);
		GlStateManager.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MIN_FILTER, framebufferFilterIn);
		GlStateManager.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_MAG_FILTER, framebufferFilterIn);
		GlStateManager.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_WRAP_S, GLC.GL_CLAMP);
		GlStateManager.texParameter(GLC.GL_TEXTURE_2D, GLC.GL_TEXTURE_WRAP_T, GLC.GL_CLAMP);
		GlStateManager.bindTexture(0);
	}
	
	public static enum Fbo {
		BASE,
		ARB,
		EXT,
		NONE;
	}
}
