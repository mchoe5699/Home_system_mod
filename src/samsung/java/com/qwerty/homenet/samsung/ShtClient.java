package com.qwerty.homenet.samsung;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Samsung.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ShtClient {
 @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Samsung.BE.get(),Renderer::new);}
 public static void receive(ShtNetwork.Data packet){var mc=Minecraft.getInstance();if(packet.snapshot().open())mc.setScreen(new ShtScreen(packet.snapshot(),packet.remote()));else if(mc.screen instanceof ShtScreen s&&s.getPos().equals(packet.snapshot().pos()))s.update(packet.snapshot());}
 public static final class Renderer implements BlockEntityRenderer<Sht101c>{
  private final com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
  public Renderer(BlockEntityRendererProvider.Context c){}
  public boolean shouldRenderOffScreen(Sht101c s){return true;}
  public void render(Sht101c s,float partial,PoseStack p,MultiBufferSource out,int light,int overlay){p.pushPose();var delta=com.qwerty.homenet.move.DeviceOffsets.get(s.getLevel(),s.getBlockPos());p.translate(delta.x,delta.y,delta.z);Direction f=s.getBlockState().getValue(com.qwerty.homenet.block.WallMountedBlock.FACING);switch(f){case NORTH-> {p.translate(.5,.30,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,.30,0);case EAST->{p.translate(0,.30,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,.30,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}
   p.scale(ShtMesh.SCALE,ShtMesh.SCALE,ShtMesh.SCALE);p.translate(-500,0,0);ShtMesh.draw(p,out,light,s.docked(),s.folded(),false);
   var at=s.videoCamera();if(at!=null)video.drawWall(p,out,at,802,740-317,578,740-124,44,2,2,com.qwerty.homenet.video.VideoFormat.MONOCHROME);
   String logo=s.productLogo.text();if(!logo.equals("SAMSUNG")){ShtMesh.tint(p,out,254,86,174,30,62,0xf3efdd,light);p.pushPose();p.translate(265,740-107,63);p.scale(.8f,-.8f,.8f);Minecraft.getInstance().font.drawInBatch(logo,0,0,0x3d4942,false,p.last().pose(),out,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,light);p.popPose();}p.popPose();
  }
 }
 private static BlockEntityWithoutLevelRenderer items;
 public static BlockEntityWithoutLevelRenderer itemRenderer(){if(items==null){var mc=Minecraft.getInstance();items=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
  public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack p,MultiBufferSource out,int light,int overlay){p.pushPose();boolean remote=stack.is(Samsung.CORDLESS.get());float scale=remote?.00125f:.00082f;p.translate(remote?.35:.09,remote?.1:.15,.4);p.scale(scale,scale,scale);ShtMesh.draw(p,out,light,true,true,remote);p.popPose();}
 };}return items;}
}
