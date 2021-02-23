package com.backsun.lod.proxy;

import org.lwjgl.opengl.GL11;

import com.backsun.lod.builders.LodBuilder;
import com.backsun.lod.objects.LodChunk;
import com.backsun.lod.objects.LodDimension;
import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.objects.LodWorld;
import com.backsun.lod.renderer.LodRenderer;
import com.backsun.lod.util.LodConfig;
import com.backsun.lodCore.util.RenderGlobalHook;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

//TODO Find a way to replace getIntegratedServer so this mod could be used on non-local worlds.
// Minecraft.getMinecraft().getIntegratedServer()

/**
 * This handles all events sent to the client,
 * and is the starting point for most of this program.
 * 
 * @author James_Seibel
 * @version 02-23-2021
 */
public class ClientProxy extends CommonProxy
{
	private LodRenderer renderer;
	private LodWorld lodWorld;
	private LodBuilder lodBuilder;
	
	public ClientProxy()
	{
		lodBuilder = new LodBuilder();
	}
	
	
	
	
	//==============//
	// render event //
	//==============//
	
	@SubscribeEvent
	public void renderWorldLast(RenderWorldLastEvent event)
	{
		RenderGlobalHook.endRenderingStencil();
		GL11.glStencilFunc(GL11.GL_EQUAL, 0, 0xFF);
		
		if (LodConfig.drawLODs)
			renderLods(event.getPartialTicks());
		
		GL11.glDisable(GL11.GL_STENCIL_TEST);
	}
	
	/**
	 * Do any setup that is required to draw LODs
	 * and then tell the LodRenderer to draw.
	 */
	public void renderLods(float partialTicks)
	{
		int newWidth = Math.max(4, (Minecraft.getMinecraft().gameSettings.renderDistanceChunks * LodChunk.WIDTH * 2) / LodRegion.SIZE);
		if (lodWorld != null && lodBuilder.regionWidth != newWidth)
		{
			lodWorld.resizeDimensionRegionWidth(newWidth);
			lodBuilder.regionWidth = newWidth;
			
			// skip this frame, hopefully the lodWorld
			// should have everything set up by then
			return;
		}
		
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null || mc.player == null || lodWorld == null)
			return;
		
		int dimId = mc.player.dimension;
		LodDimension lodDim = lodWorld.getLodDimension(dimId);
		if (lodDim == null)
			return;
		
		
		double playerX = mc.player.posX;
		double playerZ = mc.player.posZ;
		
		int xOffset = ((int)playerX / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterX();
		int zOffset = ((int)playerZ / (LodChunk.WIDTH * LodRegion.SIZE)) - lodDim.getCenterZ();
		
		if (xOffset != 0 || zOffset != 0)
		{
			lodDim.move(xOffset, zOffset);
		}
		
		// we wait to create the renderer until the first frame
		// to make sure that the EntityRenderer has
		// been created, that way we can get the fovModifer
		// method from it through reflection.
		if (renderer == null)
		{
			renderer = new LodRenderer();
		}
		else
		{
			renderer.drawLODs(lodDim, partialTicks);
		}
	}	
	
	
	
	
	
	//===============//
	// update events //
	//===============//
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent event)
	{
		lodWorld = lodBuilder.generateLodChunkAsync(event.getChunk());
	}
	
	/**
	 * this event is called whenever a chunk is created for the first time.
	 */
	@SubscribeEvent
	public void onChunkPopulate(PopulateChunkEvent event)
	{
		Minecraft mc = Minecraft.getMinecraft();
		if (mc != null && event != null)
		{
			WorldClient world = mc.world;
			
			if(world != null)
			{
				lodWorld = lodBuilder.generateLodChunkAsync(world.getChunkFromChunkCoords(event.getChunkX(), event.getChunkZ()));
			}
		}
	}
	
	
	
	
}
