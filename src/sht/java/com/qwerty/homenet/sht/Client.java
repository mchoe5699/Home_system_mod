package com.qwerty.homenet.sht;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Product.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class Client {
 static void receive(Wire.View v){var mc=Minecraft.getInstance();if(v.state().open())mc.setScreen(new Controls(v.state(),v.remote()));else if(mc.screen instanceof Controls c&&c.data.pos().equals(v.state().pos()))c.data=v.state();}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Product.TYPE.get(),c->new Renderer());}
 static class Renderer implements BlockEntityRenderer<Station>{
  com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
  public boolean shouldRenderOffScreen(Station b){return true;}
  public void render(Station b,float time,PoseStack p,MultiBufferSource out,int light,int overlay){p.pushPose();var offset=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(offset.x,offset.y,offset.z);switch(b.getBlockState().getValue(com.qwerty.homenet.block.WallMountedBlock.FACING)){case NORTH->{p.translate(.5,.30,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,.30,0);case EAST->{p.translate(0,.30,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,.30,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(Surface.SCALE,Surface.SCALE,Surface.SCALE);p.translate(-523.5,0,0);Surface.draw(p,out,light,false,b.docked,b.folded);Surface.bodyPose(p);var cam=b.camera();if(cam!=null)video.drawWall(p,out,cam,770,700-318,578,700-174,66,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);int[] ys={251,231,270,307,192,535,498,575};for(int i=0;i<8;i++)if(i!=4&&(b.lights()&(1<<i))!=0)Surface.patch(p,out,460,ys[i]-2,12,4,73,0xbbce75,15728880);
   if(!b.alarm.isEmpty()&&!b.alarm.equals("emergency")){int lens=b.alarm.equals("fire")?328:b.alarm.equals("gas")?376:420;Surface.patch(p,out,lens-6,188,12,8,73,b.alarm.equals("fire")?0xef6655:b.alarm.equals("gas")?0xe8cf59:0x7ad174,15728880);}
   var logo=b.productLogo.text();if(!logo.equals("SAMSUNG")){Surface.patch(p,out,306,131,143,26,77,0xf1efdf,light);p.pushPose();p.translate(310,700-137,78);p.scale(.7f,-.7f,.7f);Minecraft.getInstance().font.drawInBatch(logo,0,0,0x536050,false,p.last().pose(),out,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,light);p.popPose();}p.popPose();p.popPose();}
 }
 static BlockEntityWithoutLevelRenderer renderer;
 static net.minecraftforge.client.extensions.common.IClientItemExtensions itemExtension(){return new net.minecraftforge.client.extensions.common.IClientItemExtensions(){public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var mc=Minecraft.getInstance();renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
  public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack p,MultiBufferSource out,int light,int overlay){boolean radio=stack.is(Product.RADIO.get());p.pushPose();p.translate(radio?.36:-.02,radio?.45:.18,.5);float s=radio?.00078f:.0009f;p.scale(s,s,s);Surface.draw(p,out,light,radio,true,true);p.popPose();}
 };}return renderer;}};}
}
