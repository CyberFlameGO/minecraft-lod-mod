package backsun.lod.renderer;

import java.awt.Color;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import backsun.lod.objects.LoadedRegions;
import backsun.lod.objects.LodChunk;
import backsun.lod.util.OfConfig;
import backsun.lod.util.enums.ColorDirection;
import backsun.lod.util.enums.LodLocation;
import backsun.lod.util.fog.FogMode;
import backsun.lod.util.fog.FogType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * @author James Seibel
 * @version 1-20-2021
 */
public class LodRenderer
{
	public boolean debugging = true;
	
	private Minecraft mc;
	private float farPlaneDistance;
	// make sure this is an even number, or else it won't align with the chunk grid
	public static final int VIEW_DISTANCE_MULTIPLIER = 12;
	public static final int LOD_WIDTH = 16;
	public static final int MINECRAFT_CHUNK_WIDTH = 16;
	
	public int defaultLodHeight = 0;
	
	private Tessellator tessellator;
	private BufferBuilder bufferBuilder;
	
	private OfConfig ofConfig;
	
	public LoadedRegions regions = null;
	
	
	
	public LodRenderer()
	{
		mc = Minecraft.getMinecraft();
		
		tessellator = Tessellator.getInstance();
		bufferBuilder = tessellator.getBuffer();
		
		ofConfig = new OfConfig();
		
		// GL11.GL_MODELVIEW  //5888
		// GL11.GL_PROJECTION //5889
	}
	
	
	
	public void drawLODs(Minecraft mc, float partialTicks)
	{
		if (ofConfig.fovMethod == null)
		{
			// don't continue if we can't get the
			// user's FOV
			return;
		}
		
		if (regions == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		
		
		// used for debugging and viewing how long different processes take
		mc.world.profiler.startSection("LOD setup");
		@SuppressWarnings("unused")
		long startTime = System.nanoTime();
		
		// color setup
		int alpha = 255; // 0 - 255
		@SuppressWarnings("unused")
		Color error = new Color(255, 0, 225, alpha); // bright pink
		
		Color red = new Color(255, 0, 0, alpha);
		Color black = new Color(0, 0, 0, alpha);
		Color white = new Color(255, 255, 255, alpha);
		Color invisible = new Color(0,0,0,0);
		
		//
		AxisAlignedBB defaultLodBB = new AxisAlignedBB(0, defaultLodHeight, 0, LOD_WIDTH, defaultLodHeight, LOD_WIDTH);
		
		
		
		// get the camera location
		Entity player = mc.player;
		double cameraX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
		double cameraY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
		double cameraZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
		
		int playerXChunkOffset = ((int) cameraX / MINECRAFT_CHUNK_WIDTH) * MINECRAFT_CHUNK_WIDTH;
		int playerZChunkOffset = ((int) cameraZ / MINECRAFT_CHUNK_WIDTH) * MINECRAFT_CHUNK_WIDTH;
		
		
		
		// determine how far the game's render distance is currently set
		farPlaneDistance = mc.gameSettings.renderDistanceChunks * MINECRAFT_CHUNK_WIDTH;
		
		// set how big the LODs will be and how far they will go
		int totalLength = (int) farPlaneDistance * VIEW_DISTANCE_MULTIPLIER;
		int numbOfBoxesWide = (totalLength / LOD_WIDTH);
		
		// this where we will start drawing squares
		// (exactly half the total width)
		int startX = (-LOD_WIDTH * (numbOfBoxesWide / 2)) + playerXChunkOffset;
		int startZ = (-LOD_WIDTH * (numbOfBoxesWide / 2)) + playerZChunkOffset;
		
		
		// this is where we store the LOD objects
		AxisAlignedBB lodArray[] = new AxisAlignedBB[numbOfBoxesWide * numbOfBoxesWide];
		// this is where we store the color for each LOD object
		Color colorArray[] = new Color[numbOfBoxesWide * numbOfBoxesWide];
		
		
		
		
		
		//=================//
		// create the LODs //
		//=================//
		
		mc.world.profiler.endStartSection("LOD generation");
		
		
		// x axis
		for (int i = 0; i < numbOfBoxesWide; i++)
		{
			// z axis
			for (int j = 0; j < numbOfBoxesWide; j++)
			{
				// set where this square will be drawn in the world
				double xOffset = -cameraX + // start at x = 0
							(LOD_WIDTH * i) + // offset by the number of LOD blocks
							startX; // offset so the center LOD block is centered underneath the player
				double zOffset = -cameraZ + (LOD_WIDTH * j) + startZ;
				
				int chunkX = i + (startX / MINECRAFT_CHUNK_WIDTH);
				int chunkZ = j + (startZ / MINECRAFT_CHUNK_WIDTH);
				
				LodChunk lod = regions.getLodFromCoordinates(chunkX, chunkZ); // new LodChunk(); //   
				
				if (lod == null)
				{
					// note: for some reason if any color or lod object are set here
					// it causes the game to use 100% gpu, all of it undefined in the debug menu
					// and drop to ~6 fps.
					colorArray[i + (j * numbOfBoxesWide)] = null;
					lodArray[i + (j * numbOfBoxesWide)] = null;
					
					continue;
				}
				
				Color c = lod.colors[ColorDirection.TOP.index];
				
				double yOffset = -cameraY;
				
				// if debugging draw the squares as a black and white checker board
				if (debugging)
				{
					if ((chunkX + chunkZ) % 2 == 0)
						c = white;
					else
						c = black;
					// draw the first square as red
					if (i == 0 && j == 0)
						c = red;
					
					colorArray[i + (j * numbOfBoxesWide)] = c;
				}
				
				
				// skip the middle
				// (As the player moves some chunks will overlap or be missing,
				// this is just how chunk loading/unloading works. This can hopefully
				// be hidden with careful use of fog)
				int middle = numbOfBoxesWide / 2;
				int width = mc.gameSettings.renderDistanceChunks;
				if ((i > middle - width && i < middle + width) && (j > middle - width && j < middle + width))
				{
					// add the color to the array
					colorArray[i + (j * numbOfBoxesWide)] = null;
					
					// add the new box to the array
					lodArray[i + (j * numbOfBoxesWide)] = null;
				}
				else
				{
					// add the color to the array
					colorArray[i + (j * numbOfBoxesWide)] = c;
					
					// add the new box to the array
					lodArray[i + (j * numbOfBoxesWide)] = new AxisAlignedBB(0, lod.bottom[LodLocation.NE.value], 0, LOD_WIDTH, lod.top[LodLocation.NE.value], LOD_WIDTH).offset(xOffset, yOffset, zOffset);
				}
			}
		}
		
		
		
		
		
		
		
		
		
		//===========================//
		// GL settings for rendering //
		//===========================//
		
		// set the new model view matrix
		setProjectionMatrix(partialTicks);
		
		// set the required open GL settings
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glLineWidth(2.0f);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_BLEND);
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		mc.world.profiler.endStartSection("LOD build buffer");
		// send the LODs over to the GPU
		sendToGPUAndDraw(lodArray, colorArray, cameraX, cameraY ,cameraZ);
		
		
		
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		mc.world.profiler.endStartSection("LOD cleanup");
		
		
		// this must be done otherwise other parts of the screen may be drawn with a fog effect
		// IE the GUI
		GlStateManager.disableFog();
		
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
		GL11.glPolygonOffset(-1.f, -1.f);
		
		
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		// TODO re-set the modelview matrix
		
		// This is about how long this whole process should take
		// 16 ms = 60 hz
		@SuppressWarnings("unused")
		long endTime = System.nanoTime();
		
		// end of profiler tracking
		mc.world.profiler.endSection();
	}
	
	



	/**
	 * create a new projection matrix and send it over to the GPU
	 * @param partialTicks how many ticks into the frame we are
	 */
	private void setProjectionMatrix(float partialTicks)
	{
		// create a new view frustum so that the squares can be drawn outside the normal view distance
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		// farPlaneDistance // 10 chunks = 160	
		
		
		// only continue if we can get the FOV
		if (ofConfig.fovMethod != null)
		{
			Project.gluPerspective(ofConfig.getFov(mc, partialTicks, true), (float) mc.displayWidth / (float) mc.displayHeight, 0.05f, farPlaneDistance * VIEW_DISTANCE_MULTIPLIER);
		}
	}
	
	
	/**
	 * draw an array of cubes (or squares) with the given colors.
	 * @param bbArray bounding boxes to draw
	 * @param colorArray color of each box to draw
	 */
	private void sendToGPUAndDraw(AxisAlignedBB[] bbArray, Color[] colorArray, double cameraX, double cameraY, double cameraZ)
	{
		int red;
		int green;
		int blue;
		int alpha;
				
		bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		
		int numbDrawn = 0;
		
		int colorIndex = 0;
		for (AxisAlignedBB bb : bbArray)
		{
			if (bb != null && colorArray[colorIndex] != null)
			{
				numbDrawn++; // XXX
				
				// get the color of this LOD object
				red = colorArray[colorIndex].getRed();
				green = colorArray[colorIndex].getGreen();
				blue = colorArray[colorIndex].getBlue();
				alpha = colorArray[colorIndex].getAlpha();
				
				bufferBuilder.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				bufferBuilder.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
				
				// only draw the other 5 sides if there is some thickness to this box
				if (bb.minY != bb.maxY)
				{
					bufferBuilder.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					
					bufferBuilder.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					
					bufferBuilder.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
					
					bufferBuilder.pos(bb.minX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.minX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.minX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.minX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
					
					bufferBuilder.pos(bb.maxX, bb.minY, bb.minZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.minY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.maxY, bb.maxZ).color(red, green, blue, alpha).endVertex();
					bufferBuilder.pos(bb.maxX, bb.maxY, bb.minZ).color(red, green, blue, alpha).endVertex();
				}
			}
			// so we can get the next color
			colorIndex++;			
		}
		
		setupFog(FogMode.NEAR, ofConfig.getFogType());
		
		mc.world.profiler.endStartSection("LOD draw");
		
		// draw the LODs
		tessellator.draw();
		
		System.out.println(numbDrawn);
	}
	
	private void setupFog(FogMode fogMode, FogType fogType)
	{
		if(fogMode == FogMode.NONE || fogType == FogType.OFF)
		{
			GlStateManager.disableFog();
			return;
		}
		
		
		if(fogMode == FogMode.NEAR)
		{
			// the multipliers are percentages
			// of the normal view distance.
			
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.
			
			if (fogType == FogType.FANCY || fogType == FogType.UNKNOWN)
			{
				GlStateManager.setFogEnd(farPlaneDistance * 2.0f);
				GlStateManager.setFogStart(farPlaneDistance * 2.25f);
			}
			else //if(fogType == FogType.FAST)
			{
				GlStateManager.setFogEnd(farPlaneDistance * 0.8f);
				GlStateManager.setFogStart(farPlaneDistance * 1.5f);
			}
		}
		else //if(fogMode == FogMode.FAR)
		{
			// the multipliers are percentages of
			// the LOD view distance.
			
			if (fogType == FogType.FANCY || fogType == FogType.UNKNOWN)
			{
				GlStateManager.setFogStart(farPlaneDistance * 0.5f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
				GlStateManager.setFogEnd(farPlaneDistance * 1.0f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
			}
			else //if(fogType == FogType.FAST)
			{
				GlStateManager.setFogStart(farPlaneDistance * 0.5f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
				GlStateManager.setFogEnd(farPlaneDistance * 0.8f * VIEW_DISTANCE_MULTIPLIER / 2.0f);
			}
		}
		
		GlStateManager.setFogDensity(0.1f);
		GlStateManager.enableFog();
	}
	
	
	
	
	
	
	
}