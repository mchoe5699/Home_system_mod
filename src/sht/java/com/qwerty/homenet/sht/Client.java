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
 @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->{GeometryCache.clear();Ht512Visual.keys=null;DeviceVisibility.clear();Surface.models.clear();Surface.solids=null;PortableVisual.parts=null;PortableVisual.keys=null;});}
 static void address(Wire.Address a){Minecraft.getInstance().setScreen(new AddressControls(a.pos(),a.unit()));}
 static void receive(Wire.View v){var mc=Minecraft.getInstance();if(v.state().open())mc.setScreen(v.accessory()||v.remote()&&v.state().flag("wired")?new PortableControls(v.state()):new Controls(v.state(),v.remote()));else if(mc.screen instanceof Controls c&&c.data.pos().equals(v.state().pos()))c.data=v.state();}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Product.HT100_TYPE.get(),c->new Ht100Visual.Renderer());e.registerBlockEntityRenderer(Product.HT512_TYPE.get(),c->new Ht512Visual.Renderer());e.registerBlockEntityRenderer(Product.CHARGER_TYPE.get(),c->new PortableVisual.Renderer());e.registerBlockEntityRenderer(Product.TYPE.get(),c->new Renderer());e.registerBlockEntityRenderer(Product.TYPE102.get(),c->new Renderer());}
 static void devicePose(Station b,PoseStack p){var offset=com.qwerty.homenet.move.DeviceOffsets.get(b.getLevel(),b.getBlockPos());p.translate(offset.x,offset.y,offset.z);switch(b.getBlockState().getValue(com.qwerty.homenet.block.WallMountedBlock.FACING)){case NORTH->{p.translate(.5,.30,1);p.mulPose(Axis.YP.rotationDegrees(180));}case SOUTH->p.translate(.5,.30,0);case EAST->{p.translate(0,.30,.5);p.mulPose(Axis.YP.rotationDegrees(90));}case WEST->{p.translate(1,.30,.5);p.mulPose(Axis.YP.rotationDegrees(-90));}default->{}}p.scale(Surface.SCALE,Surface.SCALE,Surface.SCALE);p.translate(-523.5,0,0);}
 static class Renderer implements BlockEntityRenderer<Station>{
  com.qwerty.homenet.client.kvm.KvmCameraView video=new com.qwerty.homenet.client.kvm.KvmCameraView();
  public boolean shouldRenderOffScreen(Station b){return false;}
  public void render(Station b,float time,PoseStack p,MultiBufferSource out,int light,int overlay){Surface.select(b.wired());if(DeviceVisibility.hidden(b,GeometryCache.bounds(Surface.meshKey(false,b.docked,b.folded)),pose->devicePose(b,pose)))return;p.pushPose();devicePose(b,p);Surface.draw(p,out,light,false,b.docked,b.folded);Surface.bodyPose(p);var cam=b.camera();if(cam!=null){p.pushPose();p.scale(16,16,16);video.drawWall(p,out,cam,770,700-318,578,700-174,66,b.brightness,b.contrast,com.qwerty.homenet.video.VideoFormat.MONOCHROME);p.popPose();}for(var lamp:Indicators.forModel(b.wired()))if((b.lights()&lamp.bit())!=0)Surface.indicator(p,out,lamp);

   var logo=b.productLogo.text();if(!logo.equals("SAMSUNG")){Surface.patch(p,out,306,131,143,26,77,0xf1efdf,light);p.pushPose();p.translate(310,700-137,78);p.scale(.7f,-.7f,.7f);Minecraft.getInstance().font.drawInBatch(logo,0,0,0x536050,false,p.last().pose(),out,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,light);p.popPose();}p.popPose();p.popPose();}
 }
 static BlockEntityWithoutLevelRenderer renderer;
 static net.minecraftforge.client.extensions.common.IClientItemExtensions itemExtension(){return new net.minecraftforge.client.extensions.common.IClientItemExtensions(){public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null){var mc=Minecraft.getInstance();renderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
  public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack p,MultiBufferSource out,int light,int overlay){if(stack.is(Product.HT100_ITEM.get())){p.pushPose();p.translate(.5,.12,.5);p.scale(.0013f,.0013f,.0013f);p.translate(-Ht100Visual.W/2,Ht100Visual.H-700,0);Ht100Visual.draw(p,out,light,true);p.popPose();return;}if(stack.is(Product.HT512_ITEM.get())){p.pushPose();p.translate(.5,.13,.5);p.scale(.0012f,.0012f,.0012f);p.translate(-Ht512Visual.W/2,Ht512Visual.H-700,0);Ht512Visual.draw(p,out,light);p.popPose();return;}
   if(stack.is(Product.PORTABLE102.get())){p.pushPose();p.translate(.4,.15,.4);p.scale(.0008f,.0008f,.0008f);p.translate(0,200,0);PortableVisual.handset(p,out,light,stack.hasTag()&&(stack.getTag().getBoolean("ringing")||stack.getTag().getBoolean("talking")),stack.hasTag()&&stack.getTag().getBoolean("intercom"));p.popPose();return;}
   if(stack.is(Product.CHARGER_ITEM.get())){p.pushPose();p.translate(.3,.1,.3);p.scale(.001f,.001f,.001f);PortableVisual.stand(p,out,light,true,false,false);p.popPose();return;}
   boolean radio=stack.is(Product.RADIO.get());Surface.select(stack.is(Product.BASE102_ITEM.get()));p.pushPose();p.translate(radio?.36:-.02,radio?.45:.18,.5);float s=radio?.00078f:.0009f;p.scale(s,s,s);Surface.draw(p,out,light,radio,true,!stack.is(Product.BASE102_ITEM.get()));p.popPose();}
 };}return renderer;}};}
}
