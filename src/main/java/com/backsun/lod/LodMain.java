package com.backsun.lod;

import com.backsun.lod.proxy.ClientProxy;
import com.backsun.lod.proxy.CommonProxy;
import com.backsun.lod.util.Reference;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * Initialize and setup the Mod.
 * <br>
 * If you are looking for the real start of the mod
 * check out the ClientProxy.
 * 
 * @author James Seibel
 * @version 02-07-2021
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.backsun.lod.asm"})
@Mod(modid = Reference.MOD_ID, name = Reference.NAME, version = Reference.VERSION, dependencies = "required-after:lodcore@[1.0,)")
public class LodMain
{
	@Instance
	public static LodMain instance;
	
	@SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.COMMON_PROXY_CLASS)
	public static CommonProxy common_proxy;
	public static ClientProxy client_proxy;
	
	@EventHandler
	public static void PreInit(FMLPreInitializationEvent event)
	{
		Minecraft.getMinecraft().getFramebuffer().enableStencil();
	}
	
	@EventHandler
	public static void Init(FMLInitializationEvent event)
	{
		MinecraftForge.EVENT_BUS.register(common_proxy);
		client_proxy = new ClientProxy();
	}
	
	@EventHandler
	public static void PostInit(FMLPostInitializationEvent event)
	{
		
	}
}
