package com.backsun.lod.util;

import com.backsun.lod.objects.LodRegion;
import com.backsun.lod.objects.RegionPos;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

/**
 * This class holds methods that may be used in multiple places.
 * 
 * @author James Seibel
 * @version 04-01-2021
 */
public class LodUtils
{
	private static Minecraft mc = Minecraft.getInstance();
	
	
	
	/**
	 * Gets the first valid ServerWorld.
	 * 
	 * @return null if there are no ServerWorlds
	 */
	public static ServerWorld getFirstValidServerWorld()
	{
		if (mc.getIntegratedServer() == null)
			return null;
		
		Iterable<ServerWorld> worlds = mc.getIntegratedServer().getWorlds();
		
		for (ServerWorld world : worlds)
			return world;
				
		return null;
	}
	
	/**
	 * Gets the ServerWorld for the relevant dimension.
	 * 
	 * @return null if there is no ServerWorld for the given dimension
	 */
	public static ServerWorld getServerWorldFromDimension(DimensionType dimension)
	{
		IntegratedServer server = mc.getIntegratedServer();
		if (server == null)
			return null;
		
		Iterable<ServerWorld> worlds = server.getWorlds();
		ServerWorld returnWorld = null;
		
		for (ServerWorld world : worlds)
		{
			if(world.getDimensionType() == dimension)
			{
				returnWorld = world;
				break;
			}
		}
				
		return returnWorld;
	}
	
	/**
	 * Convert the given ChunkPos into a RegionPos.
	 */
	public static RegionPos convertChunkPosToRegionPos(ChunkPos pos)
	{
		RegionPos rPos = new RegionPos();
		rPos.x = pos.x / LodRegion.SIZE;
		rPos.z = pos.z / LodRegion.SIZE;
		
		// prevent issues if X/Z is negative and less than 16
		if (pos.x < 0)
		{
			rPos.x = (Math.abs(rPos.x) * -1) - 1; 
		}
		if (pos.z < 0)
		{
			rPos.z = (Math.abs(rPos.z) * -1) - 1; 
		}
		
		return rPos;
	}
	
	/**
	 * Return whether the given chunk
	 * has any data in it.
	 */
	public static boolean chunkHasBlockData(IChunk chunk)
	{
		ChunkSection[] blockStorage = chunk.getSections();
		
		for(ChunkSection section : blockStorage)
		{
			if(section != null && !section.isEmpty())
			{
				return true;
			}
		}
		
		return false;
	}
	
	
	
	public static String getCurrentDimensionID()
	{

		Minecraft mc = Minecraft.getInstance();
		
		if(mc.isIntegratedServerRunning())
		{
			// this will return the world save location
			// and the dimension folder
			
			if(mc.world == null)
				return "";
			
			ServerWorld serverWorld = LodUtils.getServerWorldFromDimension(mc.world.getDimensionType());
			if(serverWorld == null)
				return "";
			
			ServerChunkProvider provider = serverWorld.getChunkProvider();
			if(provider == null)
				return "";
			
			return provider.getSavedData().folder.toString();
		}
		else
		{
			ServerData server = mc.getCurrentServerData();
			return server.serverName + ", IP " + 
					server.serverIP + ", GameVersion " + 
					server.gameVersion.getString() + "\\"
					+ "dim_" + mc.world.getDimensionType().getEffects().getPath() + "\\";
		}
	}

	
	/**
	 * If on single player this will return the name of the user's
	 * world and the dimensional save folder, if in multiplayer 
	 * it will return the server name, game version, and dimension.<br>
	 * <br>
	 * This can be used to determine where to save files for a given
	 * dimension.
	 */
	public static String getDimensionIDFromWorld(IWorld world)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if(mc.isIntegratedServerRunning())
		{
			// this will return the world save location
			// and the dimension folder
			
			ServerWorld serverWorld = LodUtils.getServerWorldFromDimension(world.getDimensionType());
			if(serverWorld == null)
				throw new NullPointerException("getDimensionIDFromWorld wasn't able to get the ServerWorld for the dimension " + world.getDimensionType().getEffects().getPath());
			
			ServerChunkProvider provider = serverWorld.getChunkProvider();
			if(provider == null)
				throw new NullPointerException("getDimensionIDFromWorld wasn't able to get the ServerChunkProvider for the dimension " + world.getDimensionType().getEffects().getPath());
			
			return provider.getSavedData().folder.toString();
		}
		else
		{
			ServerData server = mc.getCurrentServerData();
			return server.serverName + ", IP " + 
					server.serverIP + ", GameVersion " + 
					server.gameVersion.getString() + "\\"
					+ "dim_" + world.getDimensionType().getEffects().getPath() + "\\";
		}
	}
	
	/**
	 * If on single player this will return the name of the user's
	 * world, if in multiplayer it will return the server name
	 * and game version.
	 */
	public static String getWorldID(IWorld world)
	{
		if(mc.isIntegratedServerRunning())
		{
			// chop off the dimension ID as it is not needed/wanted
			String dimId = getDimensionIDFromWorld(world);
			
			// get the world name
			int saveIndex = dimId.indexOf("saves") + 1 + "saves".length();
			int slashIndex = dimId.indexOf('\\', saveIndex);
			dimId = dimId.substring(saveIndex, slashIndex);
			return dimId;
		}
		else
		{
			ServerData server = mc.getCurrentServerData();
			return server.serverName + ", IP " + 
					server.serverIP + ", GameVersion " + 
					server.gameVersion.getString();
		}
	}
	
	
}
